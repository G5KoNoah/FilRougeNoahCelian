package ca.uqac.mobile.projetfilrouge.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import ca.uqac.mobile.projetfilrouge.AlarmScheduler
import ca.uqac.mobile.projetfilrouge.AppVibrator
import ca.uqac.mobile.projetfilrouge.MainActivity
import ca.uqac.mobile.projetfilrouge.R
import ca.uqac.mobile.projetfilrouge.data.SharedAlarm
import kotlinx.serialization.json.Json

// Appelé par le système quand une alarme planifiée par AlarmScheduler arrive à échéance
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarm = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM)
            ?.let { Json.decodeFromString(SharedAlarm.serializer(), it) }
        alarm?.let { AlarmScheduler(context).schedule(it) }

        //Par default tout activer
        val sound = alarm?.sound ?: true
        val vibrations = alarm?.vibrations ?: true

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        // Le canal ne gère plus que le son. La vibration passe par AppVibrator (usage "alarme") :
        // celle d'un canal sans son est traitée comme une vibration de notification, et le téléphone peut la couper.
        val channelId = if (sound) CHANNEL_SOUND else CHANNEL_SILENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notification_channel_alarm),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                if (sound) {
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                } else {
                    setSound(null, null)
                }
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (vibrations) AppVibrator(context).alarm()

        // Toucher la notification ouvre l'application ; MainActivity arrête alors la vibration
        val openAppIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_STOP_VIBRATION, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Balayer la notification pour l'effacer : StopAlarmReceiver arrête la vibration
        val dismissIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, StopAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(intent.getStringExtra("name")?.ifBlank { null } ?: context.getString(R.string.notification_alarm_title))
            .setContentText(context.getString(R.string.notification_alarm_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent)
            .setDeleteIntent(dismissIntent)
            .setAutoCancel(true)
            .apply {
                // Android 7 et moins : pas de canaux, le son se règle sur la notification
                // (ignoré à partir d'Android 8, où c'est le canal qui décide)
                if (sound) setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) else setSilent(true)
            }
            .build()

        // Le son boucle jusqu'à ce que l'utilisateur touche la notification
        if (sound) {
            notification.flags = notification.flags or Notification.FLAG_INSISTENT
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val EXTRA_STOP_VIBRATION = "stop_vibration"

        private const val CHANNEL_SOUND = "alarm_sound_v3"
        private const val CHANNEL_SILENT = "alarm_silent_v3"


    }
}