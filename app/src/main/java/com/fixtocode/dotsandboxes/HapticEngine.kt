package com.fixtocode.dotsandboxes

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

class HapticEngine(context: Context) {

    var isEnabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val hasAmplitudeControl: Boolean = vibrator?.hasAmplitudeControl() == true

    fun uiTick(view: View? = null) {
        if (!isEnabled) return
        val vib = vibrator
        try {
            if (vib != null && vib.hasVibrator()) {
                val amp = if (hasAmplitudeControl) 70 else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(12L, amp))
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        } catch (e: Exception) {
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun lineSnap(view: View? = null) {
        if (!isEnabled) return
        val vib = vibrator
        try {
            if (vib != null && vib.hasVibrator()) {
                val amp = if (hasAmplitudeControl) 150 else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(20L, amp))
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        } catch (e: Exception) {
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun botStep() {
        if (!isEnabled) return
        val vib = vibrator ?: return
        try {
            if (vib.hasVibrator()) {
                val amp = if (hasAmplitudeControl) 55 else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(10L, amp))
            }
        } catch (e: Exception) {}
    }

    fun boxCaptured(comboCount: Int, boxesClosedAtOnce: Int) {
        if (!isEnabled) return
        val vib = vibrator ?: return
        try {
            if (vib.hasVibrator()) {
                val baseAmp = (160 + (comboCount * 18)).coerceAtMost(255)
                if (hasAmplitudeControl) {
                    if (boxesClosedAtOnce >= 2) {
                        val timings = longArrayOf(0L, 28L, 30L, 35L, 30L, 50L)
                        val amplitudes = intArrayOf(0, baseAmp, 0, baseAmp, 0, 255)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        val timings = longArrayOf(0L, 24L, 28L, 42L)
                        val firstAmp = (baseAmp * 3 / 4).coerceIn(1, 255)
                        val amplitudes = intArrayOf(0, firstAmp, 0, baseAmp)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                } else {
                    val timings = longArrayOf(0L, 25L, 30L, 45L)
                    vib.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            }
        } catch (e: Exception) {}
    }

    fun victoryFanfare() {
        if (!isEnabled) return
        val vib = vibrator ?: return
        try {
            if (vib.hasVibrator()) {
                if (hasAmplitudeControl) {
                    val timings = longArrayOf(0L, 40L, 50L, 40L, 50L, 40L, 60L, 110L)
                    val amplitudes = intArrayOf(0, 140, 0, 180, 0, 220, 0, 255)
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    val timings = longArrayOf(0L, 40L, 50L, 40L, 50L, 40L, 60L, 110L)
                    vib.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            }
        } catch (e: Exception) {}
    }
}