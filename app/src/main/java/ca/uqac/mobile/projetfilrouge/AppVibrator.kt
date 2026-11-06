package ca.uqac.mobile.projetfilrouge

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// Vibrations de l'app, via l'applicationContext pour pouvoir les arrêter de partout
class AppVibrator(context: Context) {

    private val vibrator: Vibrator? = context.applicationContext.let { app ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Vibrator::class.java)
        }
    }

    private val peutVibrer: Boolean get() = vibrator?.hasVibrator() == true

    // Vibration courte au toucher (Android 10+)
    fun click() {
        val v = vibrator ?: return
        if (!peutVibrer) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }

    // Vibration d'alarme répétée pendant durationMs, même en mode silencieux
    fun alarm(durationMs: Long = ALARM_DURATION_MS) {
        val v = vibrator ?: return
        if (!peutVibrer) return

        val cycles = (durationMs / (VIBRATE_MS + PAUSE_MS)).toInt().coerceAtLeast(1)
        val pattern = LongArray(1 + cycles * 2).also { p ->
            for (i in 0 until cycles) {
                p[1 + i * 2] = VIBRATE_MS
                p[2 + i * 2] = PAUSE_MS
            }
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                v.vibrate(
                    VibrationEffect.createWaveform(pattern, -1),
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                @Suppress("DEPRECATION")
                v.vibrate(
                    VibrationEffect.createWaveform(pattern, -1),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                )
            }
            else -> {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, -1)
            }
        }
    }

    // Arrête la vibration en cours
    fun stop() {
        vibrator?.cancel()
    }

    private companion object {
        const val VIBRATE_MS = 800L
        const val PAUSE_MS = 400L
        const val ALARM_DURATION_MS = 60_000L
    }
}
