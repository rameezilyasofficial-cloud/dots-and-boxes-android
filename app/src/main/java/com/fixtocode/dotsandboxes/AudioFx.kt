package com.fixtocode.dotsandboxes

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

class AudioFx {
    private val sampleRate = 44100
    var isEnabled: Boolean = true

    private val synthPool = Executors.newFixedThreadPool(4)

    private val comboScale = doubleArrayOf(
        523.25, // C5
        587.33, // D5
        659.25, // E5
        783.99, // G5
        880.00, // A5
        1046.50, // C6
        1174.66, // D6
        1318.51, // E6
        1567.98  // G6
    )

    private fun playBuffer(buffer: ShortArray, durationMs: Int) {
        if (!isEnabled) return
        synthPool.execute {
            var track: AudioTrack? = null
            try {
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 30L)
            } catch (e: Exception) {
            } finally {
                try {
                    track?.stop()
                } catch (e: Exception) {}
                try {
                    track?.release()
                } catch (e: Exception) {}
            }
        }
    }

    fun uiClick() {
        if (!isEnabled) return
        val durationMs = 28
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 1100.0 - (720.0 * progress)
            phase += 2.0 * Math.PI * freq / sampleRate
            val env = exp(-6.5 * progress)
            val sample = sin(phase) * env * 0.35
            buffer[i] = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
        }
        playBuffer(buffer, durationMs)
    }

    fun snap() {
        if (!isEnabled) return
        val durationMs = 65
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 480.0 * (0.27).pow(progress)
            phase1 += 2.0 * Math.PI * freq / sampleRate
            phase2 += 2.0 * Math.PI * (freq * 2.4) / sampleRate

            val attack = (i / (sampleRate * 0.003)).coerceAtMost(1.0)
            val decay = exp(-5.2 * progress)
            val env = attack * decay

            val wave = (sin(phase1) * 0.75 + sin(phase2) * 0.25) * env * 0.55
            buffer[i] = (wave * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
        }
        playBuffer(buffer, durationMs)
    }

    fun botSnap() {
        if (!isEnabled) return
        val durationMs = 55
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 280.0 + (140.0 * progress)
            phase += 2.0 * Math.PI * freq / sampleRate
            val env = exp(-4.8 * progress)
            val wave = (sin(phase) * 0.7 + sin(phase * 2.0) * 0.3) * env * 0.42
            buffer[i] = (wave * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
        }
        playBuffer(buffer, durationMs)
    }

    fun boxClaimed(comboCount: Int = 1, boxesClosedAtOnce: Int = 1) {
        if (!isEnabled) return
        val durationMs = if (boxesClosedAtOnce >= 2) 280 else 210
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val scaleIdx = (comboCount - 1).coerceIn(0, comboScale.size - 1)
        val rootFreq = comboScale[scaleIdx]
        val fifthFreq = rootFreq * 1.4983
        val octaveFreq = rootFreq * 2.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples

            val attack = (i / (sampleRate * 0.006)).coerceAtMost(1.0)
            val decay = exp(-4.2 * progress)
            val env = attack * decay

            val activeRoot = if (boxesClosedAtOnce >= 2 && progress > 0.38) rootFreq * 1.2599 else rootFreq

            val fundamental = sin(2.0 * Math.PI * activeRoot * t) * 0.55
            val fifth = sin(2.0 * Math.PI * fifthFreq * t) * 0.28
            val octave = sin(2.0 * Math.PI * octaveFreq * t) * 0.17

            val signal = (fundamental + fifth + octave) * env * 0.65
            buffer[i] = (signal * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
        }
        playBuffer(buffer, durationMs)
    }

    fun victory() {
        if (!isEnabled) return
        val durationMs = 680
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteStarts = doubleArrayOf(0.0, 0.11, 0.22, 0.34)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var mix = 0.0

            for (n in notes.indices) {
                if (t >= noteStarts[n]) {
                    val localT = t - noteStarts[n]
                    val localEnv = (localT / 0.008).coerceAtMost(1.0) * exp(-4.5 * localT)
                    val f = notes[n]
                    val tone = sin(2.0 * Math.PI * f * localT) * 0.65 +
                               sin(2.0 * Math.PI * (f * 2.0) * localT) * 0.25 +
                               sin(2.0 * Math.PI * (f * 1.5) * localT) * 0.10
                    mix += tone * localEnv
                }
            }

            val clamped = (mix * 0.42 * 32767.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = clamped.toShort()
        }
        playBuffer(buffer, durationMs)
    }
}