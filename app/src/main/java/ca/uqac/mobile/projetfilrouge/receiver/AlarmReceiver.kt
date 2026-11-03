package ca.uqac.mobile.projetfilrouge.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ca.uqac.mobile.projetfilrouge.AlarmScheduler
import ca.uqac.mobile.projetfilrouge.AlarmSoundService
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
        val title = intent.getStringExtra("name")

        if (vibrations) AppVibrator(context).alarm()

        if (sound) {
            AlarmSoundService.start(context, title, alarm?.ringtoneUrl)
        } else {
            context.getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(context, title))
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val EXTRA_STOP_VIBRATION = "stop_vibration"

        private const val CHANNEL_ID = "alarm_v4"
        private val OUTDATED_CHANNELS = listOf("alarm_channel", "alarm_sound_v3", "alarm_silent_v3")

        fun buildNotification(context: Context, title: String?): Notification {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                OUTDATED_CHANNELS.forEach { notificationManager.deleteNotificationChannel(it) }
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_alarm),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Toucher la notification ouvre l'application ; MainActivity arrête alors la vibration
            val openAppIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).putExtra(EXTRA_STOP_VIBRATION, true),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            // Balayer la notification pour l'effacer : StopAlarmReceiver arrête la vibration
            val stopIntent = PendingIntent.getBroadcast(
                context, 0, Intent(context, StopAlarmReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title?.ifBlank { null } ?: context.getString(R.string.notification_alarm_title))
                .setContentText(context.getString(R.string.notification_alarm_text))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppIntent)
                .setDeleteIntent(stopIntent)
                .addAction(0, context.getString(R.string.notification_alarm_stop), stopIntent)
                .setAutoCancel(true)
                .build()
        }
    }
}
