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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import ca.uqac.mobile.projetfilrouge.data.Friend
import ca.uqac.mobile.projetfilrouge.data.FriendRepository

// Alarmes de l'utilisateur, profils des membres et votes en cours
class SharedAlarmsViewModel(application: Application) : AndroidViewModel(application) {

    // Texte traduit depuis les ressources
    private fun str(@StringRes id: Int): String = getApplication<Application>().getString(id)

    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> = _uid.asStateFlow()

    private val _alarms = MutableStateFlow<List<SharedAlarm>>(emptyList())
    val alarms: StateFlow<List<SharedAlarm>> = _alarms.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val memberProfiles: StateFlow<Map<String, Friend>> = _alarms
        .map { alarms -> alarms.flatMap { it.members }.distinct().sorted() }
        .distinctUntilChanged()
        .flatMapLatest { uids -> FriendRepository.observeProfiles(uids).catch { emit(emptyList()) } }
        .map { profiles -> profiles.associateBy { it.uid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

    // Relance les écoutes pour le nouvel utilisateur
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
                    observePollsOf(alarms.map { it.id }.sorted())
                }
        }
    }

    // Écoute les votes ouverts de ces alarmes si la liste a changé
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

    // Active ou désactive l'alarme pour moi
    fun setEnabled(alarm: SharedAlarm, enabled: Boolean) {
        val me = _uid.value ?: return
        launchAction(str(R.string.alarm_error_change)) { AlarmRepository.setEnabled(alarm.id, me, enabled) }
    }

    // Lance une action en affichant l'erreur éventuelle
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
