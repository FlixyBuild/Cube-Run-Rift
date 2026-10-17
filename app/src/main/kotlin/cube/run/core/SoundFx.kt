package cube.run.core

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import cube.run.data.MusicStyle
import cube.run.data.Settings
import cube.run.data.Worlds
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedurally synthesized sound effects, shared by every game.
 *
 * Available sound names (pitch-shift with `rate` 0.5..2.0 for variety):
 *  tap, blip, pop, place, perfect, combo, success, fail,
 *  whoosh, boom, coin, rise, slide, fanfare, drain, bell, growl,
 *  and Outer Space's: moonjump, moonland, flyby, stardust, hum (a loop)
 */
object SoundFx {
    // Investigation hooks: inactive in ordinary runs and release builds.
    @Volatile var testMutedName: String? = null
    @Volatile var testObserver: ((String, Long, Long, Int) -> Unit)? = null
    private const val SR = 44100
    private const val PHONK_FUNK_MUSIC = "music_phonk_funk"
    private const val URDU_THEME_MUSIC = "music_urdu_theme"
    // Loop controls need the pool as well as the queued one-shot playback worker.
    @Volatile private var pool: SoundPool? = null
    private val ids = HashMap<String, Int>()
    @Volatile private var ready = false
    private data class Playback(val name: String, val id: Int, val volume: Float, val rate: Float)
    private var playback: SoundPlaybackQueue<Playback>? = null

    private var initializing = false
    @Volatile private var desiredMusicWorld = Worlds.NEON_CITY_ID
    @Volatile private var musicStream = 0
    @Volatile private var musicStreamName: String? = null
    private val musicLock = Any()

    @Synchronized fun init(ctx: Context) {
        if (initializing) return
        initializing = true
        thread(name = "sfx-load") {
            var localPool: SoundPool? = null
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                val soundPool = SoundPool.Builder().setMaxStreams(12).setAudioAttributes(attrs).build()
                localPool = soundPool
                pool = soundPool
                playback = SoundPlaybackQueue { effect ->
                    if (Settings.soundEnabled && Settings.effectsVolume > 0f) {
                        val observer = if (cube.run.BuildConfig.DEBUG) testObserver else null
                        val before = if (observer != null) System.nanoTime() else 0L
                        val v = (effect.volume * Settings.effectsVolume).coerceIn(0f, 1f)
                        val stream = soundPool.play(effect.id, v, v, 1, 0, effect.rate)
                        observer?.invoke(effect.name, before, System.nanoTime(), stream)
                    }
                }
                // A separate versioned cache regenerates the low-register SFX mix after updates.
                val dir = File(ctx.applicationContext.cacheDir, "sfx-bass-v2").apply { mkdirs() }
                val effectNames = listOf("tap", "blip", "pop", "place", "perfect", "combo", "success", "fail", "whoosh", "boom", "coin", "rise", "slide", "fanfare", "drain", "bell", "growl",
                    "moonjump", "moonland", "flyby", "stardust", "hum")
                val musicNames = Worlds.all.indices.map { "music_$it" } + listOf(PHONK_FUNK_MUSIC, URDU_THEME_MUSIC)
                val names = effectNames + musicNames
                // Versioned names regenerate only the samples whose original synthesis changed.
                fun file(name: String) = File(dir, when {
                    name == "coin" -> "coin-chime-v3.wav"
                    name == "stardust" -> "stardust-v3.wav"
                    name == PHONK_FUNK_MUSIC -> "$name-loop-v3.wav"
                    name == URDU_THEME_MUSIC -> "$name-loop-v1.wav"
                    name.startsWith("music_") -> "$name-loop-v2.wav"
                    else -> "$name.wav"
                })
                if (effectNames.any { !file(it).isFile || file(it).length() == 0L }) {
                    for ((name, pcm) in synthAll()) if (!file(name).isFile || file(name).length() == 0L) file(name).writeBytes(wav(pcm))
                }
                for (name in musicNames) {
                    if (!file(name).isFile || file(name).length() == 0L) {
                        val pcm = when (name) {
                            PHONK_FUNK_MUSIC -> PhonkFunkMusic.synthesize()
                            URDU_THEME_MUSIC -> UrduThemeMusic.synthesize()
                            else -> synthMusic(name.substringAfterLast('_').toInt())
                        }
                        file(name).writeBytes(wav(pcm))
                    }
                }
                val loaded = java.util.concurrent.atomic.AtomicInteger(0)
                val submitted = java.util.concurrent.atomic.AtomicBoolean(false)
                fun publish() {
                    if (submitted.get() && loaded.get() >= names.size) {
                        ready = true
                        refreshMusicSettings()
                    }
                }
                soundPool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status != 0) Log.e("CubeRunAudio", "Sample load failed id=$sampleId status=$status")
                    loaded.incrementAndGet(); publish()
                }
                for (name in names) ids[name] = soundPool.load(file(name).path, 1)
                submitted.set(true); publish()
            } catch (failure: Throwable) {
                Log.e("CubeRunAudio", "Audio setup failed; gameplay will continue silently", failure)
                playback?.close(); playback = null
                pool = null; ready = false; ids.clear()
                localPool?.release()
                synchronized(this@SoundFx) { initializing = false }
            }
        }
    }

    fun play(name: String, rate: Float = 1f, vol: Float = 1f) {
        if (!ready || !Settings.soundEnabled || Settings.effectsVolume <= 0f) return
        if (cube.run.BuildConfig.DEBUG && name == testMutedName) return
        val id = ids[name] ?: return
        playback?.offer(Playback(name, id, vol.coerceIn(0f, 1f), rate.coerceIn(0.5f, 2f)))
    }

    /** Start [name] looping at [vol]; returns 0 while muted/not loaded, so the caller can retry. */
    fun loop(name: String, vol: Float): Int {
        if (!ready || !Settings.soundEnabled || Settings.effectsVolume <= 0f) return 0
        val id = ids[name] ?: return 0
        val v = (vol.coerceIn(0f, 1f) * Settings.effectsVolume).coerceIn(0f, 1f)
        return pool?.play(id, v, v, 2, -1, 1f) ?: 0
    }

    fun setVolume(stream: Int, vol: Float) {
        if (stream == 0) return
        val v = (vol.coerceIn(0f, 1f) * Settings.effectsVolume).coerceIn(0f, 1f)
        pool?.setVolume(stream, v, v)
    }

    fun stop(stream: Int) { if (stream != 0) pool?.stop(stream) }

    /** Select the original procedural loop that belongs to the live world. */
    fun setMusicWorld(worldId: Int) {
        desiredMusicWorld = Worlds.get(worldId).id
        refreshMusicSettings()
    }

    /** Apply volume/toggle changes without restarting the current musical phrase. */
    fun refreshMusicSettings() = synchronized(musicLock) {
        val p = pool ?: return@synchronized
        val audible = ready && Settings.soundEnabled && Settings.musicEnabled && Settings.musicVolume > 0f
        if (!audible) {
            if (musicStream != 0) p.stop(musicStream)
            musicStream = 0; musicStreamName = null
            return@synchronized
        }
        val musicName = when (Settings.musicStyle) {
            MusicStyle.PHONK_FUNK -> PHONK_FUNK_MUSIC
            MusicStyle.URDU_THEME -> URDU_THEME_MUSIC
            MusicStyle.WORLD_MIX -> "music_${desiredMusicWorld}"
        }
        if (musicStream != 0 && musicStreamName == musicName) {
            val v = Settings.musicVolume.coerceIn(0f, 1f)
            p.setVolume(musicStream, v, v)
            return@synchronized
        }
        if (musicStream != 0) p.stop(musicStream)
        val id = ids[musicName] ?: return@synchronized
        val v = Settings.musicVolume.coerceIn(0f, 1f)
        musicStream = p.play(id, v, v, 0, -1, 1f)
        musicStreamName = musicName.takeIf { musicStream != 0 }
    }

    /** Keep platform audio out of the foreground while Android backgrounds the game. */
    fun pause() { pool?.autoPause() }
    fun resume() { if (Settings.soundEnabled) { pool?.autoResume(); refreshMusicSettings() } }

    // ------------------------------------------------------------------ synth

    private fun synthAll(): List<Pair<String, ShortArray>> = listOf(
        "tap" to synth(40) { t, p ->
            (sin(t * 420.0 * TAU) * .78 + sin(t * 210.0 * TAU) * .22) * decay(p, 5.0)
        },
        "blip" to synth(70) { t, p ->
            (square(t * (360.0 + 260.0 * p)) * .34 + sin(t * (180.0 + 130.0 * p) * TAU) * .32) * decay(p, 3.0)
        },
        "pop" to synth(70) { t, p ->
            (sin(t * (230.0 + 760.0 * p * p) * TAU) * .78 + sin(t * 115.0 * TAU) * .22) * decay(p, 4.0)
        },
        "place" to synth(90) { t, p ->
            (tri(t * 145.0) * 0.8 + noise() * 0.35) * decay(p, 5.0)
        },
        "perfect" to synth(260) { t, p ->
            val f = 880.0
            (sin(t * f * TAU) + 0.42 * sin(t * f * 2.0 * TAU) + 0.18 * sin(t * f * 3.0 * TAU) +
                0.30 * sin(t * 220.0 * TAU)) / 1.75 * decay(p, 4.5)
        },
        "combo" to synth(140) { t, p ->
            (sin(t * 440.0 * TAU + 3.0 * sin(t * 18.0 * TAU)) * .76 + sin(t * 110.0 * TAU) * .24) * decay(p, 3.0)
        },
        "success" to arpeggio(doubleArrayOf(349.23, 440.00, 523.25, 698.46), 120, 460),
        "fail" to synth(420) { t, p ->
            (saw(t * (320.0 - 200.0 * p)) * 0.7 + noise() * 0.12 * p + sin(t * 80.0 * TAU) * .14) * decay(p, 2.2)
        },
        "whoosh" to lowpassed(190, 0.10) { _, p ->
            noise() * sin(p * PI).pow(1.4)
        },
        "growl" to houndGrowl(),
        "boom" to lowpassed(380, 0.035) { t, p ->
            (noise() * 0.9 + sin(t * 64.0 * TAU) * 0.8) * decay(p, 3.2)
        },
        "coin" to synth(150) { t, p -> // warmer two-note coin chime with an audible lower octave
            val f = if (t < 0.035) 392.0 else 523.25
            val sub = if (t < 0.035) 98.0 else 130.81
            (sin(t * f * TAU) * .58 + sin(t * sub * TAU) * .34 + sin(t * f * 2 * TAU) * .08) *
                .62 * decay(p, 3.2)
        },
        "rise" to synth(300) { t, p ->
            (sin(t * (170.0 + 620.0 * p.pow(1.5)) * TAU) + .24 * sin(t * 85.0 * TAU)) *
                (0.6 + 0.4 * sin(t * 20.0 * TAU)) * sin(p * PI).pow(0.5)
        },
        "slide" to lowpassed(130, 0.22) { _, p -> noise() * sin(p * PI).pow(0.8) * 0.8 },
        "fanfare" to fanfare(),
        "drain" to drain(),
        "moonjump" to moonJump(),
        "moonland" to moonLand(),
        "flyby" to flyby(),
        "stardust" to stardust(),
        "hum" to hum(),
        "bell" to synth(1400, vol = 0.8) { t, p -> // a singing bowl: a soft strike and inharmonic partials ringing out
            val f = 392.0
            (sin(t * f * TAU) + 0.5 * sin(t * f * 2.76 * TAU) * exp(-p * 3.0) + 0.25 * sin(t * f * 5.4 * TAU) * exp(-p * 6.0)) /
                1.75 * (0.85 + 0.15 * sin(t * 4.5 * TAU)) * decay(p, 3.4)
        },
    )

    private const val TAU = 2.0 * PI
    private val rng = Random(42)

    private fun decay(p: Double, k: Double) = exp(-p * k) * min(1.0, p * 60.0)
    private fun square(cycles: Double) = if (cycles - cycles.toLong() < 0.5) 1.0 else -1.0
    private fun saw(cycles: Double) = 2.0 * (cycles - cycles.toLong()) - 1.0
    private fun tri(cycles: Double) = 1.0 - 4.0 * abs((cycles - cycles.toLong()) - 0.5)
    private fun noise() = rng.nextDouble() * 2.0 - 1.0

    /** gen receives (t seconds, progress 0..1) and returns -1..1 */
    private fun synth(ms: Int, vol: Double = 1.0, gen: (Double, Double) -> Double): ShortArray {
        val n = SR * ms / 1000
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val p = i.toDouble() / n
            val v = (gen(t, p) * vol).coerceIn(-1.0, 1.0)
            out[i] = (v * 30000).toInt().toShort()
        }
        return out
    }

    /** Like synth but runs the signal through a one-pole lowpass (alpha = cutoff feel). */
    private fun lowpassed(ms: Int, alpha: Double, gen: (Double, Double) -> Double): ShortArray {
        val n = SR * ms / 1000
        val out = ShortArray(n)
        var acc = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val p = i.toDouble() / n
            acc += alpha * (gen(t, p) - acc)
            out[i] = (acc.coerceIn(-1.0, 1.0) * 30000).toInt().toShort()
        }
        return out
    }

    /**
     * Original four-bar electronic cue composed offline at app startup. Each world receives its own
     * key, tempo and motif; the short PCM loop is cached in app-private storage and never uses
     * third-party music. Transients and a short seam blend keep SoundPool looping click-free.
     */
    private fun synthMusic(worldId: Int): ShortArray {
        val roots = doubleArrayOf(110.0, 130.81, 98.0, 146.83, 110.0, 82.41, 116.54, 98.0, 130.81, 116.54)
        val bpms = intArrayOf(120, 132, 112, 124, 118, 108, 126, 112, 136, 118)
        val chords = arrayOf(
            intArrayOf(0, 7, 9, 5), intArrayOf(0, 5, 3, 7), intArrayOf(0, 3, 8, 5),
            intArrayOf(0, 7, 5, 9), intArrayOf(0, 5, 7, 3), intArrayOf(0, 3, 5, 10),
            intArrayOf(0, 7, 5, 2), intArrayOf(0, 3, 7, 10), intArrayOf(0, 5, 7, 9),
            intArrayOf(0, 5, 7, 9),
        )
        val motifs = arrayOf(
            intArrayOf(0, -1, 2, -1, 4, 2, -1, 5, 4, -1, 2, -1, 1, 2, 4, -1),
            intArrayOf(0, 2, -1, 4, 7, -1, 5, 4, 2, -1, 4, 5, 7, -1, 4, 2),
            intArrayOf(0, -1, 3, 4, -1, 2, -1, 5, 3, -1, 2, -1, 0, 2, -1, 4),
            intArrayOf(0, 2, 4, -1, 5, 4, 2, -1, 0, -1, 2, 4, 7, 5, 4, -1),
            intArrayOf(0, -1, 2, 4, 5, -1, 4, 2, 0, 2, -1, 4, 5, 4, 2, -1),
            intArrayOf(0, -1, 3, -1, 5, 3, -1, 2, 0, -1, 2, -1, 3, 5, 3, -1),
            intArrayOf(0, 2, -1, 4, 5, -1, 7, 5, 4, -1, 2, 4, 7, -1, 5, 2),
            intArrayOf(0, -1, 2, 3, -1, 5, 3, -1, 0, 2, -1, 5, 3, -1, 2, 0),
            intArrayOf(0, 2, 4, 5, 7, -1, 5, 4, 2, 4, 5, 7, 9, -1, 7, 5),
            intArrayOf(0, -1, 2, 4, 5, 4, -1, 2, 0, 2, 4, -1, 5, 4, 2, -1),
        )
        val scale = intArrayOf(0, 2, 3, 5, 7, 10)
        val id = worldId.coerceIn(0, roots.lastIndex)
        val root = roots[id]
        val beatSeconds = 60.0 / bpms[id]
        val samples = (SR * (16.0 * beatSeconds)).toInt()
        val out = ShortArray(samples)
        var noiseState = 0x51f15e1d xor (id * 0x45d9f3b)
        val metallic = id == Worlds.RAILWAY_ID || id == Worlds.HIGHWAY_ID
        val jungle = id == Worlds.JUNGLE_ID
        val beach = id == Worlds.BEACH_ID
        for (i in 0 until samples) {
            val t = i.toDouble() / SR
            val beat = t / beatSeconds
            val beatIndex = beat.toInt()
            val beatPhase = beat - beatIndex
            val bar = (beatIndex / 4 + id) % 4
            val chordRoot = root * 2.0.pow(chords[id][bar] / 12.0)
            val swell = .78 + .22 * sin(TAU * t / (beatSeconds * 4.0))
            val pad = (sin(TAU * chordRoot * t) + .54 * sin(TAU * chordRoot * 1.5 * t) +
                .28 * sin(TAU * chordRoot * 2.0 * t)) * .075 * swell
            val bass = sin(TAU * root * t) * exp(-beatPhase * 4.5) * .27 +
                sin(TAU * root * .5 * t) * exp(-beatPhase * 3.2) * .08

            val halfBeat = beat * 2.0
            val step = halfBeat.toInt() % motifs[id].size
            val stepPhase = halfBeat - halfBeat.toInt()
            val note = motifs[id][step]
            val lead = if (note < 0) 0.0 else {
                val f = root * 2.0.pow((12 + scale[note % scale.size]) / 12.0)
                val env = exp(-stepPhase * 5.0) * min(1.0, stepPhase * 75.0)
                (sin(TAU * f * t) + .24 * sin(TAU * f * 2.0 * t)) * env * .17
            }
            val kick = sin(TAU * (38.0 * beatPhase + 18.0 * beatPhase * beatPhase)) * exp(-beatPhase * 17.0) * .32
            noiseState = noiseState * 1664525 + 1013904223
            val noise = (noiseState ushr 8).toDouble() / 8388607.5 - 1.0
            val snare = if (beatIndex % 4 == 2) noise * exp(-beatPhase * 28.0) * .12 else 0.0
            val hatPhase = halfBeat - halfBeat.toInt()
            val hat = noise * exp(-hatPhase * 72.0) * .035
            val color = when {
                metallic -> sin(TAU * root * 4.0 * t) * exp(-beatPhase * 13.0) *
                    (if (beatIndex % 4 == 3) .075 else 0.0)
                jungle -> (sin(TAU * root * 2.0 * t) + .22 * sin(TAU * root * 3.1 * t)) *
                    exp(-beatPhase * 18.0) * (if (beatIndex % 4 == 1 || beatIndex % 4 == 3) .08 else 0.0)
                beach -> (sin(TAU * root * 3.0 * t) + .35 * sin(TAU * root * 4.0 * t)) *
                    exp(-beatPhase * 14.0) * (if (beatIndex % 4 == 1 || beatIndex % 4 == 3) .07 else .025)
                else -> 0.0
            }
            val mix = (pad + bass + lead + kick + snare + hat + color) / 1.34
            out[i] = (mix.coerceIn(-1.0, 1.0) * 21000.0).toInt().toShort()
        }
        val fade = (SR / 80).coerceAtMost(samples / 4)
        for (i in 0 until fade) {
            val t = (i + 1.0) / fade
            val tail = out[samples - fade + i].toDouble()
            val head = out[i].toDouble()
            out[i] = (tail * (1.0 - t) + head * t).toInt().toShort()
        }
        return out
    }

    /** Overlapping note arpeggio, soft square + sine blend. */
    private fun arpeggio(freqs: DoubleArray, noteMs: Int, totalMs: Int): ShortArray {
        val n = SR * totalMs / 1000
        val out = DoubleArray(n)
        val noteN = SR * noteMs / 1000
        for ((k, f) in freqs.withIndex()) {
            val start = k * (noteN * 3 / 4)
            for (i in 0 until (noteN * 2).coerceAtMost(n - start)) {
                val t = i.toDouble() / SR
                val p = i.toDouble() / (noteN * 2)
                out[start + i] += (sin(t * f * TAU) * 0.7 + square(t * f) * 0.18) * decay(p, 3.5) * 0.8
            }
        }
        return ShortArray(n) { (out[it].coerceIn(-1.0, 1.0) * 30000).toInt().toShort() }
    }

    /**
     * The jackpot: a bell chord struck note by note (C E G C E) that rings on
     * with a slow shimmer. Sines only, so it stays round on speakers and recordings.
     */
    private fun fanfare(): ShortArray {
        val n = SR * 1700 / 1000
        val out = DoubleArray(n)
        val notes = doubleArrayOf(392.0, 493.88, 587.33, 783.99, 987.77)
        for ((k, f) in notes.withIndex()) {
            val start = k * SR * 70 / 1000
            for (i in 0 until n - start) {
                val t = i.toDouble() / SR
                val p = i.toDouble() / (n - start)
                val shimmer = 1.0 + 0.004 * sin(t * 5.5 * TAU)
                val bell = sin(t * f * shimmer * TAU) + 0.3 * sin(t * f * 2.0 * TAU) * exp(-t * 6.0) + 0.12 * sin(t * f * 3.01 * TAU) * exp(-t * 9.0)
                out[start + i] += bell * decay(p, 2.6) * 0.23
            }
        }
        return ShortArray(n) { (out[it].coerceIn(-1.0, 1.0) * 30000).toInt().toShort() }
    }

    /** Original low, rasped synth growl used for the hound's advance and lunge cues. */
    private fun houndGrowl(): ShortArray {
        var phase = 0.0
        var rasp = 0.0
        return lowpassed(700, 0.16) { t, p ->
            val pitch = 54.0 + 17.0 * sin(t * 4.2 * TAU) + 11.0 * (1.0 - p)
            phase += pitch / SR
            rasp += 0.075 * (noise() - rasp)
            val envelope = sin(p * PI).pow(0.48) * (0.82 + 0.18 * sin(t * 19.0 * TAU))
            (sin(phase * TAU) * 0.72 + sin(phase * 2.06 * TAU) * 0.19 + rasp * 0.4) * envelope * 0.62
        }
    }

    /**
     * The void swallowing the shop: a swirling tone that sinks from a whistle to a rumble
     * over 1.3 s and stops dead where the gulp lands.
     */
    private fun drain(): ShortArray {
        var phase = 0.0
        var swirl = 0.0
        return lowpassed(1300, 0.2) { _, p ->
            val f = 55.0 + 520.0 * (1.0 - p).pow(2.2)
            phase += f / SR
            swirl += (4.0 + 16.0 * p) / SR
            val env = p.pow(0.45) * (1.0 - ((p - 0.93) / 0.07).coerceIn(0.0, 1.0))
            (sin(phase * TAU) * 0.75 * (0.65 + 0.35 * sin(swirl * TAU)) + noise() * 0.3 * p) * env
        }
    }

    // ------------------------------------------------------------ outer space

    /** A low-gravity take-off: a soft, airy sine that rises and floats away. */
    private fun moonJump(): ShortArray {
        var phase = 0.0
        var acc = 0.0
        return synth(340) { t, p ->
            phase += (240.0 + 320.0 * p.pow(0.6)) / SR
            acc += 0.06 * (noise() - acc)
            val env = (p / 0.06).coerceAtMost(1.0) * (1.0 - p).pow(1.4)
            (sin(phase * TAU + 0.15 * sin(t * 9.0 * TAU)) * 0.55 + acc * 0.5) * env
        }
    }

    /** A low-gravity landing: a cushioned thump, no crack. */
    private fun moonLand(): ShortArray {
        var phase = 0.0
        var acc = 0.0
        return synth(260) { _, p ->
            phase += (130.0 - 60.0 * p) / SR
            acc += 0.05 * (noise() - acc)
            (sin(phase * TAU) * 0.8 + acc * 0.7) * decay(p, 5.5)
        }
    }

    /** An asteroid tumbling past: a swell of filtered air that drops in pitch as it goes by. */
    private fun flyby(): ShortArray {
        var phase = 0.0
        var acc = 0.0
        return synth(760) { _, p ->
            acc += (0.2 - 0.16 * p) * (noise() - acc)
            phase += (95.0 - 40.0 * p) / SR
            val env = sin(p * PI).pow(1.6)
            (acc * 1.3 + sin(phase * TAU) * 0.25) * env
        }
    }

    /**
     * A coin in space: one soft, round note (A5) with a slow chorus shimmer
     * from a twin a few hertz off, a breath of octave on the onset and a short
     * tail. No bright strike and no high sparkle: a long line of coins should
     * stay pleasant, never shrill.
     */
    private fun stardust(): ShortArray {
        var phase = 0.0
        var twin = 0.0
        return synth(260, vol = 0.55) { t, p ->
            val f = 880.0 * (0.985 + 0.015 * (1.0 - exp(-t * 70.0)))     // settles up into the note
            phase += f / SR; twin += (f + 4.0) / SR
            val tone = sin(phase * TAU) + 0.35 * sin(twin * TAU) + 0.16 * sin(phase * 2.0 * TAU) * exp(-t * 40.0)
            tone / 1.5 * exp(-t * 13.0) * min(1.0, t * 250.0) * min(1.0, (1.0 - p) * 10.0)
        }
    }

    /**
     * Space's hum: a calm pad (A, C#, E, with a slowly beating twin and a high
     * shimmer) and a breath of solar wind. Every partial and wobble completes
     * whole cycles in its 4 seconds, and the wind is cross-faded over its own
     * seam, so it loops without a click.
     */
    private fun hum(): ShortArray {
        val seconds = 4.0
        val n = (SR * seconds).toInt()
        val fade = SR / 2
        val wind = DoubleArray(n + fade)
        var acc = 0.0; var acc2 = 0.0
        for (i in wind.indices) { acc += 0.02 * (noise() - acc); acc2 += 0.02 * (acc - acc2); wind[i] = acc2 }
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val swell = 0.8 + 0.2 * sin(t * 0.25 * TAU)
            val pad = sin(t * 220.0 * TAU) + sin(t * 220.5 * TAU) * 0.8 + sin(t * 277.25 * TAU) * 0.55 +
                sin(t * 329.75 * TAU) * 0.6 + sin(t * 440.0 * TAU) * 0.25 * (0.5 + 0.5 * sin(t * 0.5 * TAU)) +
                sin(t * 659.25 * TAU) * 0.08 * (0.5 + 0.5 * sin(t * 0.75 * TAU + 1.0))
            // the wind's tail is blended into its head, so the loop point is seamless
            val w = if (i < fade) wind[i] * i / fade + wind[n + i] * (1.0 - i.toDouble() / fade) else wind[i]
            val v = pad * 0.16 * swell + w * 3.2
            out[i] = (v.coerceIn(-1.0, 1.0) * 30000).toInt().toShort()
        }
        return out
    }

    private fun max0(v: Double) = if (v > 0.0) v else 0.0

    // ------------------------------------------------------------------- wav

    private fun wav(pcm: ShortArray): ByteArray {
        val dataLen = pcm.size * 2
        val o = ByteArrayOutputStream(44 + dataLen)
        fun s(s: String) = o.write(s.toByteArray(Charsets.US_ASCII))
        fun i32(v: Int) {
            o.write(v and 0xFF); o.write((v shr 8) and 0xFF)
            o.write((v shr 16) and 0xFF); o.write((v shr 24) and 0xFF)
        }
        fun i16(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF) }
        s("RIFF"); i32(36 + dataLen); s("WAVE")
        s("fmt "); i32(16); i16(1); i16(1); i32(SR); i32(SR * 2); i16(2); i16(16)
        s("data"); i32(dataLen)
        for (v in pcm) { o.write(v.toInt() and 0xFF); o.write((v.toInt() shr 8) and 0xFF) }
        return o.toByteArray()
    }
}
