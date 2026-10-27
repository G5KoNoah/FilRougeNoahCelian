package ca.uqac.mobile.projetfilrouge.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ca.uqac.mobile.projetfilrouge.AlarmScheduler
import ca.uqac.mobile.projetfilrouge.data.AlarmData

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val scheduler = AlarmScheduler(application)

    fun addAlarm(alarm: AlarmData) {
        scheduler.schedule(alarm)
    }

    fun toggleAlarm(alarm: AlarmData, enabled: Boolean) {
        if (enabled) scheduler.schedule(alarm) else scheduler.cancel(alarm)
    }

    fun deleteAlarm(alarm: AlarmData) {
        scheduler.cancel(alarm)
    }
}