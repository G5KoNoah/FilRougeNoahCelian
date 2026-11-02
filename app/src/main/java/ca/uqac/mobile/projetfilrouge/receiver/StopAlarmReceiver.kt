package ca.uqac.mobile.projetfilrouge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.uqac.mobile.projetfilrouge.AppVibrator

// Appelé quand l'utilisateur balaye la notification d'alarme pour l'effacer : on arrête la vibration
class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppVibrator(context).stop()
    }
}
