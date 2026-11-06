package ca.uqac.mobile.projetfilrouge.viewModel

import ca.uqac.mobile.projetfilrouge.R
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.data.AlarmPoll
import ca.uqac.mobile.projetfilrouge.data.AlarmRepository
import ca.uqac.mobile.projetfilrouge.data.PollRepository
import ca.uqac.mobile.projetfilrouge.data.SharedAlarm
import ca.uqac.mobile.projetfilrouge.data.uidChanges
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import ca.uqac.mobile.projetfilrouge.data.FriendRepository

class SharedAlarmsViewModel(application: Application) : AndroidViewModel(application) {

    // Les ViewModels n'ont pas de stringResource : on passe par l'Application, qui suit la langue de l'appareil
    private fun str(@StringRes id: Int): String = getApplication<Application>().getString(id)

    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> = _uid.asStateFlow()

    private val _alarms = MutableStateFlow<List<SharedAlarm>>(emptyList())
    val alarms: StateFlow<List<SharedAlarm>> = _alarms.asStateFlow()

    private val _memberAvatars = MutableStateFlow<Map<String, String?>>(emptyMap())
    val memberAvatars: StateFlow<Map<String, String?>> = _memberAvatars.asStateFlow()

    // Votes ouverts sur mes alarmes : sert au compteur "votes en cours" de chaque carte
    private val _openPolls = MutableStateFlow<List<AlarmPoll>>(emptyList())
    val openPolls: StateFlow<List<AlarmPoll>> = _openPolls.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var observeJob: Job? = null
    private var pollsJob: Job? = null
    private var watchedAlarmIds: List<String>? = null

    init {
        viewModelScope.launch { uidChanges.collect { onUserChanged(it) } }
    }

    private fun onUserChanged(newUid: String?) {
        if (newUid == _uid.value && newUid != null) return

        observeJob?.cancel()
        pollsJob?.cancel()
        watchedAlarmIds = null
        _uid.value = newUid
        _alarms.value = emptyList()
        _openPolls.value = emptyList()
        _error.value = null

        if (newUid == null) {
            _error.value = str(R.string.error_no_user)
            return
        }
        observeJob = viewModelScope.launch {
            AlarmRepository.observeAlarmsOf(newUid)
                .catch { e -> _error.value = e.localizedMessage ?: str(R.string.error_server) }
                .collect { alarms ->
                    _alarms.value = alarms
                    loadMemberAvatars(alarms.flatMap { it.members }.distinct())
                    observePollsOf(alarms.map { it.id }.sorted())
                }
        }
    }

    // On ne se réabonne que si la liste des alarmes change (pas à chaque modification d'une alarme)
    private fun observePollsOf(alarmIds: List<String>) {
        if (alarmIds == watchedAlarmIds) return
        watchedAlarmIds = alarmIds
        pollsJob?.cancel()
        pollsJob = viewModelScope.launch {
            PollRepository.observeOpenPolls(alarmIds)
                .catch { }
                .collect { _openPolls.value = it }
        }
    }

    private fun loadMemberAvatars(uids: List<String>) {
        val missing = uids.filter { it !in _memberAvatars.value }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            runCatching { FriendRepository.getFriends(missing) }.onSuccess { profiles ->
                val found = profiles.associate { it.uid to it.avatarUrl }
                _memberAvatars.value = _memberAvatars.value + missing.associateWith { found[it] }
            }
        }
    }

    fun createAlarm(alarm: SharedAlarm, onCreated: (String) -> Unit = {}) {
        val me = _uid.value ?: return
        launchAction(str(R.string.alarm_error_create)) { onCreated(AlarmRepository.createAlarm(me, alarm)) }
    }

    fun updateSettings(alarm: SharedAlarm) =
        launchAction(str(R.string.alarm_error_update)) { AlarmRepository.updateSettings(alarm) }

    fun setEnabled(alarm: SharedAlarm, enabled: Boolean) {
        val me = _uid.value ?: return
        launchAction(str(R.string.alarm_error_change)) { AlarmRepository.setEnabled(alarm.id, me, enabled) }
    }

    fun addMember(alarm: SharedAlarm, friendUid: String) =
        launchAction(str(R.string.alarm_error_add_member)) { AlarmRepository.addMember(alarm.id, friendUid) }

    fun leaveOrDelete(alarm: SharedAlarm) {
        val me = _uid.value ?: return
        launchAction(str(R.string.alarm_error_remove)) {
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
