package ca.uqac.mobile.projetfilrouge

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AppVibrator(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    private val peutVibrer: Boolean get() = vibrator?.hasVibrator() == true

    // Petite vibration courte, pour un retour au toucher
    fun click() {
        val v = vibrator ?: return
        if (!peutVibrer) return
        v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    // Arrête la vibration en cours
    fun stop() {
        vibrator?.cancel()
    }
}
