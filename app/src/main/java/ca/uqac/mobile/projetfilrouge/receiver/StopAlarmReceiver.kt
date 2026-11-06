package ca.uqac.mobile.projetfilrouge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.uqac.mobile.projetfilrouge.AlarmSoundService
import ca.uqac.mobile.projetfilrouge.AppVibrator

// Arrête l'alarme depuis la notification
class StopAlarmReceiver : BroadcastReceiver() {
    // Coupe le son et la vibration
    override fun onReceive(context: Context, intent: Intent) {
        AppVibrator(context).stop()
        AlarmSoundService.stop(context)
    }
}
