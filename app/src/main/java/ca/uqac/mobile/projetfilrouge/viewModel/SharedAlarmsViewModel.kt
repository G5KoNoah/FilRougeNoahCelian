package ca.uqac.mobile.projetfilrouge.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.data.AlarmRepository
import ca.uqac.mobile.projetfilrouge.data.SharedAlarm
import ca.uqac.mobile.projetfilrouge.data.uidChanges
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SharedAlarmsViewModel : ViewModel() {

    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> = _uid.asStateFlow()

    private val _alarms = MutableStateFlow<List<SharedAlarm>>(emptyList())
    val alarms: StateFlow<List<SharedAlarm>> = _alarms.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var observeJob: Job? = null

    init {
        viewModelScope.launch { uidChanges.collect { onUserChanged(it) } }
    }

    private fun onUserChanged(newUid: String?) {
        if (newUid == _uid.value && newUid != null) return

        observeJob?.cancel()
        _uid.value = newUid
        _alarms.value = emptyList()
        _error.value = null

        if (newUid == null) {
            _error.value = "No user signed in"
            return
        }
        observeJob = viewModelScope.launch {
            AlarmRepository.observeAlarmsOf(newUid)
                .catch { e -> _error.value = e.localizedMessage ?: "Supabase error" }
                .collect { _alarms.value = it }
        }
    }

    fun createAlarm(alarm: SharedAlarm, onCreated: (String) -> Unit = {}) {
        val me = _uid.value ?: return
        launchAction("Couldn't create the alarm") { onCreated(AlarmRepository.createAlarm(me, alarm)) }
    }

    fun updateSettings(alarm: SharedAlarm) =
        launchAction("Couldn't update the alarm") { AlarmRepository.updateSettings(alarm) }

    fun setEnabled(alarm: SharedAlarm, enabled: Boolean) {
        val me = _uid.value ?: return
        launchAction("Couldn't change the alarm") { AlarmRepository.setEnabled(alarm.id, me, enabled) }
    }

    fun addMember(alarm: SharedAlarm, friendUid: String) =
        launchAction("Couldn't add this member") { AlarmRepository.addMember(alarm.id, friendUid) }

    fun leaveOrDelete(alarm: SharedAlarm) {
        val me = _uid.value ?: return
        launchAction("Couldn't remove the alarm") {
            if (alarm.ownerId == me) AlarmRepository.deleteAlarm(alarm.id)
            else AlarmRepository.removeMember(alarm.id, me)
        }
    }

    private fun launchAction(errorMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: errorMessage
            }
        }
    }
}
