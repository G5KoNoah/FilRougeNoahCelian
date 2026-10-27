package ca.uqac.mobile.projetfilrouge

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ca.uqac.mobile.projetfilrouge.data.AlarmData
import java.util.Calendar

class AlarmScheduler (private val context: Context){
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private fun pendingIntent(alarm: AlarmData): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("name", alarm.name)
        }
        return PendingIntent.getBroadcast(
            context, alarm.id, intent,       // id = requestCode
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun schedule(alarm: AlarmData) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val showIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(cal.timeInMillis, showIntent),
                pendingIntent(alarm)
            )
        } catch (e: SecurityException) {
        }
    }

    fun cancel(alarm: AlarmData) = alarmManager.cancel(pendingIntent(alarm))
}