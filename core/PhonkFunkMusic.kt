package cube.run.core

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/** A short original Brazilian-funk/phonk instrumental loop; it uses no third-party samples. */
internal object PhonkFunkMusic {
    private const val BPM = 132.0
    private const val BEATS = 16 // four bars; 8 seconds at 120 BPM
    private const val TAU = 2.0 * PI

    fun synthesize(sampleRate: Int = 44_100): ShortArray {
        require(sampleRate > 0)
        val frames = sampleRate * BEATS * 60 / BPM.toInt()
        val out = ShortArray(frames)
        val root = 55.0 // A1; a low round bass keeps the loop readable on phone speakers
        var noiseState = 0x4f1bbcdc

        for (i in out.indices) {
            val t = i.toDouble() / sampleRate
            val beat = t * BPM / 60.0
            val beatIndex = beat.toInt()
            val eighth = beat * 2.0
            val eighthIndex = eighth.toInt()
            val eighthInBar = eighthIndex % 8
            val barInLoop = beatIndex / 4 % 4
            val eighthPhase = eighth - eighthIndex

            val bassSemitone = when (eighthInBar) {
                0, 2, 4 -> 0
                3, 7 -> 7
                6 -> 3
                else -> -1
            }
            val bass = if (bassSemitone < 0) 0.0 else {
                val f = root * 2.0.pow(bassSemitone / 12.0)
                val env = exp(-eighthPhase * 5.2) * min(1.0, eighthPhase * 36.0)
                (sin(TAU * f * t) + 0.28 * sin(TAU * f * 2.0 * t)) * env * 0.31
            }

            // The kick leans into the offbeats in a tamborzão-inspired four-bar loop.
            val kickHit = eighthInBar == 0 || eighthInBar == 4 ||
                (eighthInBar == 3 && barInLoop % 2 == 0) || (eighthInBar == 7 && barInLoop % 2 == 1)
            val kick = if (kickHit) {
                val p = eighthPhase
                sin(TAU * (52.0 * p - 13.0 * p * p)) * exp(-p * 16.0) * 0.34
            } else 0.0

            noiseState = noiseState * 1_664_525 + 1_013_904_223
            val noise = (noiseState ushr 8).toDouble() / 8_388_607.5 - 1.0
            val snareHit = eighthInBar == 2 || eighthInBar == 6
            val snare = if (snareHit) {
                noise * exp(-eighthPhase * 32.0) * 0.15 +
                    sin(TAU * 185.0 * eighthPhase * 0.25) * exp(-eighthPhase * 18.0) * 0.055
            } else 0.0
            val tamborHit = eighthInBar == 1 || eighthInBar == 5 || (eighthInBar == 7 && barInLoop == 0)
            val tambor = if (tamborHit) {
                sin(TAU * (112.0 + 48.0 * (1.0 - eighthPhase)) * eighthPhase) * exp(-eighthPhase * 15.0) * 0.09 +
                    noise * exp(-eighthPhase * 34.0) * 0.025
            } else 0.0
            val hat = noise * exp(-eighthPhase * 72.0) * if (eighthInBar % 2 == 1) 0.038 else 0.012

            // Bright, clipped-metal notes give it a phonk cowbell edge without a sample.
            val bellSemitone = when (eighthInBar) {
                1 -> 7
                3 -> 10
                5 -> 12
                6 -> 7
                7 -> 3
                else -> -1
            }
            val bell = if (bellSemitone < 0) 0.0 else {
                val f = 440.0 * 2.0.pow(bellSemitone / 12.0)
                val env = exp(-eighthPhase * 9.0) * min(1.0, eighthPhase * 52.0)
                (sin(TAU * f * t) * 0.70 + sin(TAU * f * 1.49 * t) * 0.23 +
                    sin(TAU * f * 2.11 * t) * 0.07) * env * 0.10
            }

            // Short offbeat minor-key chord stabs add the funk/clavinet side of the groove.
            val chord = if (eighthInBar == 1 || eighthInBar == 5) {
                val env = exp(-eighthPhase * 8.5) * min(1.0, eighthPhase * 58.0)
                (sin(TAU * 220.0 * t) + sin(TAU * 261.63 * t) + sin(TAU * 329.63 * t)) * env * 0.045
            } else 0.0

            val mix = bass + kick + snare + tambor + hat + bell + chord
            out[i] = (tanh(mix * 1.45) * 22_000.0).toInt().toShort()
        }

        // Blend the loop point over a few milliseconds so the seeded percussion
        // does not produce a sharp transient at the repeat boundary.
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
