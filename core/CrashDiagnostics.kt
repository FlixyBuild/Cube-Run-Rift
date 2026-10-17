package cube.run.core

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Log
import cube.run.BuildConfig
import java.io.File
import java.io.PrintWriter
import kotlin.system.exitProcess

/** Crash breadcrumbs for reports that otherwise disappear with the process. */
object CrashDiagnostics {
    private const val TAG = "CubeRunCrash"
    private const val REPORT_NAME = "last-crash.txt"
    private const val EXIT_REPORT_NAME = "last-exit.txt"
    @Volatile private var installed = false

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        val app = context.applicationContext
        val file = File(app.filesDir, REPORT_NAME)
        if (file.isFile) Log.w(TAG, "Previous fatal report is retained at ${file.absolutePath} (${file.length()} bytes)")
        recordHistoricalExits(app)

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val runtime = Runtime.getRuntime()
                file.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.appendLine("Cube Run fatal exception")
                    writer.appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    writer.appendLine("android=${Build.VERSION.RELEASE} api=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}")
                    writer.appendLine("thread=${thread.name} id=${thread.id}")
                    writer.appendLine("timeMillis=${System.currentTimeMillis()}")
                    writer.appendLine("javaHeapUsed=${runtime.totalMemory() - runtime.freeMemory()} javaHeapMax=${runtime.maxMemory()}")
                    writer.appendLine("nativeHeap=${Debug.getNativeHeapAllocatedSize()} pssKb=${Debug.getPss()}")
                    error.printStackTrace(PrintWriter(writer))
                }
            } catch (writeFailure: Throwable) {
                // Diagnostics must never replace or swallow the original fatal error.
                Log.e(TAG, "Could not persist the fatal report", writeFailure)
            }
            Log.e(TAG, "Uncaught exception on ${thread.name}; forwarding to Android", error)
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
        installed = true
    }

    /**
     * OS kills and native crashes bypass the Java uncaught-exception handler.
     * API 30+ keeps a short process-exit history; reflection keeps older Android
     * versions safe while retaining reason, timestamp, memory and description.
     */
    private fun recordHistoricalExits(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
            val method = ActivityManager::class.java.getMethod(
                "getHistoricalProcessExitReasons", String::class.java,
                Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!,
            )
            val exits = (method.invoke(manager, context.packageName, 0, 8) as? List<*>)
                ?.filterNotNull().orEmpty()
            if (exits.isEmpty()) return
            val report = buildString {
                appendLine("Cube Run historical process exits (newest first)")
                appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("android=${Build.VERSION.RELEASE} api=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}")
                for ((index, exit) in exits.withIndex()) {
                    val reason = exit.readExitValue("getReason")?.toIntOrNull() ?: -1
                    appendLine("[$index] time=${exit.readExitValue("getTimestamp")} reason=$reason (${reasonName(reason)}) " +
                        "status=${exit.readExitValue("getStatus")} importance=${exit.readExitValue("getImportance")} " +
                        "pssKb=${exit.readExitValue("getPss")} rssKb=${exit.readExitValue("getRss")} " +
                        "process=${exit.readExitValue("getProcessName")} description=${exit.readExitValue("getDescription")?.replace('\n', ' ')}")
                }
            }
            File(context.filesDir, EXIT_REPORT_NAME).writeText(report, Charsets.UTF_8)
            Log.w(TAG, "Prior process exit history saved to ${File(context.filesDir, EXIT_REPORT_NAME).absolutePath}:\n$report")
        } catch (error: Throwable) {
            // Exit-reason diagnostics are best-effort and must never affect startup.
            Log.w(TAG, "Could not read Android process-exit history", error)
        }
    }

    private fun Any.readExitValue(methodName: String): String? = try {
        javaClass.getMethod(methodName).invoke(this)?.toString()
    } catch (_: Throwable) { null }

    private fun reasonName(reason: Int): String = when (reason) {
        1 -> "exit-self"
        2 -> "signaled"
        3 -> "low-memory"
        4 -> "java-crash"
        5 -> "native-crash"
        6 -> "ANR"
        7 -> "initialization-failure"
        8 -> "permission-change"
        9 -> "excessive-resource-use"
        10 -> "user-requested"
        11 -> "user-stopped"
        12 -> "dependency-died"
        13 -> "other"
        else -> "unknown"
    }

    /** Text safe to copy/paste to support; it stays on-device unless the player shares it. */
    fun reportText(context: Context): String = buildString {
        appendLine("Cube Run diagnostics")
        appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("android=${Build.VERSION.RELEASE} api=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}")
        val app = context.applicationContext
        val exits = readReport(File(app.filesDir, EXIT_REPORT_NAME))
        val fatal = readReport(File(app.filesDir, REPORT_NAME))
        appendLine()
        appendLine("Previous Android process exits:")
        appendLine(exits.ifBlank {
            "No exit history recorded (Android 10 and below do not expose this history)."
        })
        appendLine()
        appendLine("Last saved Java crash, if any:")
        appendLine(fatal.ifBlank { "No Java crash report was saved." })
    }

    private fun readReport(file: File): String = try {
        if (file.isFile) file.readText(Charsets.UTF_8).take(80_000) else ""
    } catch (_: Throwable) { "Could not read ${file.name}." }

    /** Debug-only, low-rate memory/runtime sample; useful for distinguishing a leak from an OS kill. */
    fun logHealth(source: String, state: String, liveShards: Int) {
        if (!BuildConfig.DEBUG) return
        val runtime = Runtime.getRuntime()
        val pss = try { Debug.getPss() } catch (_: Throwable) { -1 }
        Log.i(TAG, "$source $state heap=${runtime.totalMemory() - runtime.freeMemory()}/${runtime.maxMemory()} " +
            "native=${Debug.getNativeHeapAllocatedSize()} pssKb=$pss shards=$liveShards")
    }

    /** Android memory pressure callbacks are often the last useful clue before a process is reclaimed. */
    fun logMemoryPressure(level: Int, lowMemory: Boolean = false) {
        val runtime = Runtime.getRuntime()
        val pss = try { Debug.getPss() } catch (_: Throwable) { -1 }
        Log.w(TAG, "${if (lowMemory) "onLowMemory" else "onTrimMemory"} level=$level " +
            "heap=${runtime.totalMemory() - runtime.freeMemory()}/${runtime.maxMemory()} " +
            "native=${Debug.getNativeHeapAllocatedSize()} pssKb=$pss")
    }
}
