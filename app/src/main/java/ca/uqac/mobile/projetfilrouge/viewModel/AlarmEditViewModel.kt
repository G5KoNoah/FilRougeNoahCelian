package ca.uqac.mobile.projetfilrouge.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.data.AlarmRepository
import ca.uqac.mobile.projetfilrouge.data.SharedAlarm
import ca.uqac.mobile.projetfilrouge.data.currentUid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmEditViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val ringtones = listOf("Classique", "Radar", "Carillon", "Réveil doux")
    val snoozeOptions = listOf("1 x 5 min", "2 x 3 min", "3 x 5 min")
    val pollDurations = listOf("2 min", "5 min", "10 min")

    val alarmId: String? = savedStateHandle.get<String>("alarmId")
    val isNew: Boolean = alarmId == null
    private val myUid: String? = currentUid

    var isLoading by mutableStateOf(!isNew)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var ownerId by mutableStateOf(myUid.orEmpty())
        private set

    val isOwner: Boolean get() = ownerId == myUid

    var hour by mutableIntStateOf(7)
        private set

    var minute by mutableIntStateOf(0)
        private set

    var days by mutableStateOf<Set<Int>>(emptySet())
        private set

    var name by mutableStateOf("")
        private set

    var ringtone by mutableStateOf<String?>(null)
        private set

    var snooze by mutableStateOf("2 x 3 min")
        private set

    var sound by mutableStateOf(true)
        private set

    var vibrations by mutableStateOf(true)
        private set

    var members by mutableStateOf(listOfNotNull(myUid))
        private set

    var pollDuration by mutableStateOf<String?>(null)
        private set

    init {
        if (alarmId != null) {
            viewModelScope.launch {
                try {
                    val alarm = AlarmRepository.observeAlarm(alarmId).first()
                    if (alarm == null) error = "This alarm no longer exists" else fill(alarm)
                } catch (e: Exception) {
                    error = e.localizedMessage ?: "Couldn't load the alarm"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    private fun fill(alarm: SharedAlarm) {
        ownerId = alarm.ownerId
        hour = alarm.hour
        minute = alarm.minute
        days = alarm.days.toSet()
        name = alarm.name
        ringtone = alarm.ringtone.ifBlank { null }
        snooze = alarm.snooze.ifBlank { "2 x 3 min" }
        sound = alarm.sound
        vibrations = alarm.vibrations
        members = alarm.members
    }

    fun onHourChange(value: Int) {
        hour = value
    }

    fun onMinuteChange(value: Int) {
        minute = value
    }

    fun toggleDay(day: Int) {
        days = if (day in days) days - day else days + day
    }

    fun onNameChange(value: String) {
        name = value
    }

    fun onRingtoneSelected(value: String) {
        ringtone = value
    }

    fun onSnoozeSelected(value: String) {
        snooze = value
    }

    fun onSoundChange(value: Boolean) {
        sound = value
    }

    fun onVibrationsChange(value: Boolean) {
        vibrations = value
    }

    fun onPollDurationSelected(value: String) {
        pollDuration = value
    }

    fun save(onDone: () -> Unit) {
        val me = myUid ?: return
        launchSaving("Couldn't save the alarm") {
            val alarm = SharedAlarm(
                id = alarmId.orEmpty(),
                name = name.trim(),
                hour = hour,
                minute = minute,
                days = days.sorted(),
                ringtone = ringtone.orEmpty(),
                snooze = snooze,
                sound = sound,
                vibrations = vibrations,
            )
            if (alarmId == null) AlarmRepository.createAlarm(me, alarm) else AlarmRepository.updateSettings(alarm)
            onDone()
        }
    }

    fun deleteOrLeave(onDone: () -> Unit) {
        val me = myUid ?: return
        val id = alarmId ?: return
        launchSaving("Couldn't remove the alarm") {
            if (isOwner) AlarmRepository.deleteAlarm(id) else AlarmRepository.removeMember(id, me)
            onDone()
        }
    }

    private fun launchSaving(errorMessage: String, block: suspend () -> Unit) {
        if (isSaving) return
        isSaving = true
        error = null
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                error = e.localizedMessage ?: errorMessage
            } finally {
                isSaving = false
            }
        }
    }
}
