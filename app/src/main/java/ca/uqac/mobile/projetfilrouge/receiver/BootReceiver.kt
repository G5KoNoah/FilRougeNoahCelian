package ca.uqac.mobile.projetfilrouge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.uqac.mobile.projetfilrouge.AlarmSync

// Reprogramme les alarmes au démarrage du téléphone
class BootReceiver : BroadcastReceiver(){
    // Reprogramme les alarmes sauvegardées
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AlarmSync(context).rescheduleSaved()
    }
}
