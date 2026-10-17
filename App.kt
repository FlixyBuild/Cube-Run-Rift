package cube.run

import android.app.Application
import cube.run.core.Haptics
import cube.run.data.Progress
import cube.run.data.Scores
import cube.run.data.Settings
import cube.run.core.SoundFx

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        cube.run.core.CrashDiagnostics.install(this)
        cube.run.core.LaunchTrace.mark("app start")
        Scores.init(this)
        Settings.init(this)
        Progress.init(this)
        Haptics.init(this)
        SoundFx.init(this)
        cube.run.core.LaunchTrace.mark("app ready")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
            cube.run.core.CrashDiagnostics.logMemoryPressure(level)
    }

    override fun onLowMemory() {
        cube.run.core.CrashDiagnostics.logMemoryPressure(android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE, lowMemory = true)
        super.onLowMemory()
    }
}
