package cube.run.core

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/** Original sample-free Urdu-inspired instrumental loop; it uses no third-party recording or melody. */
internal object UrduThemeMusic {
    private const val BPM = 102.0
    private const val BEATS = 16 // four bars, about 9.4 seconds
    private const val TAU = 2.0 * PI
    private const val ROOT = 146.832383 // D3 drone
    // Hijaz-coloured intervals and a gently ornamented four-bar call-and-response phrase.
    private val SCALE = intArrayOf(0, 1, 4, 5, 7, 8, 11, 12, 13, 16, 17, 19)
    private val MELODY = intArrayOf(7, 6, 4, 5, 4, 3, 2, 4, 7, 8, 7, 5, 4, 2, 1, 0)

    fun synthesize(sampleRate: Int = 44_100): ShortArray {
        require(sampleRate > 0)
        val frames = sampleRate * BEATS * 60 / BPM.toInt()
        val out = ShortArray(frames)
        for (i in out.indices) {
            val t = i.toDouble() / sampleRate
            val beat = t * BPM / 60.0
            val beatIndex = beat.toInt()
            val beatPhase = beat - beatIndex
            val melodyStep = MELODY[beatIndex % MELODY.size]
            val semitone = SCALE[melodyStep]
            val note = ROOT * 2.0.pow(semitone / 12.0)
            // Plucked-string-like lead: a soft attack, quick natural decay, and warm partials.
            val attack = min(1.0, beatPhase * 34.0)
            val pluckEnv = exp(-beatPhase * 4.8) * attack
            val lead = (sin(TAU * note * t) + .28 * sin(TAU * note * 2.01 * t) +
                .10 * sin(TAU * note * 3.02 * t)) * pluckEnv * .23

            // Quiet tanpura-like root/fifth drone anchors the phrase under the melody.
            val drone = sin(TAU * (ROOT / 2.0) * t) * .075 +
                sin(TAU * ROOT * 1.5 * t + .3) * .035 +
                sin(TAU * ROOT * t) * .028

            // A low, soft hand-drum pulse marks the first and third beats of each bar.
            val inBar = beatIndex % 4
            val drumHit = inBar == 0 || inBar == 2
            val drum = if (drumHit) {
                sin(TAU * (78.0 - 25.0 * beatPhase) * t) * exp(-beatPhase * 19.0) * .12
            } else 0.0
            // Fine, original metallic taps on the offbeat; deliberately subdued for phone speakers.
            val tapHit = inBar == 1 || inBar == 3
            val tap = if (tapHit) {
                (sin(TAU * 880.0 * beatPhase) + .35 * sin(TAU * 1320.0 * beatPhase)) *
                    exp(-beatPhase * 48.0) * .025
            } else 0.0

            val mix = drone + lead + drum + tap
            out[i] = (tanh(mix * 1.45) * 22_000.0).toInt().toShort()
        }

        // Soft loop-point crossfade avoids a click while preserving the four-bar phrase.
        val fade = (sampleRate / 80).coerceAtMost(frames / 4)
        for (i in 0 until fade) {
            val amount = (i + 1.0) / fade
            val tail = out[frames - fade + i].toDouble()
            val head = out[i].toDouble()
            out[i] = (tail * (1.0 - amount) + head * amount).toInt().toShort()
        }
        return out
    }
}
