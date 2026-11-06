package ca.uqac.mobile.projetfilrouge.viewModel

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.R
import ca.uqac.mobile.projetfilrouge.data.AlarmPoll
import ca.uqac.mobile.projetfilrouge.data.AlarmRepository
import ca.uqac.mobile.projetfilrouge.data.Friend
import ca.uqac.mobile.projetfilrouge.data.FriendRepository
import ca.uqac.mobile.projetfilrouge.data.PollRepository
import ca.uqac.mobile.projetfilrouge.data.PollVote
import ca.uqac.mobile.projetfilrouge.data.SharedAlarm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Propositions et votes d'une alarme, en direct
class VoteRoomViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    // Texte traduit depuis les ressources
    private fun str(@StringRes id: Int): String = getApplication<Application>().getString(id)

    private val alarmId: String = checkNotNull(savedStateHandle["alarmId"])

    var error by mutableStateOf<String?>(null)
        private set

    var isVoting by mutableStateOf(false)
        private set

    val alarm: StateFlow<SharedAlarm?> = AlarmRepository.observeAlarm(alarmId)
        .catch { e -> error = e.localizedMessage ?: str(R.string.error_server) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val memberProfiles: StateFlow<Map<String, Friend>> = alarm
        .map { it?.members.orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { uids -> FriendRepository.observeProfiles(uids).catch { emit(emptyList()) } }
        .map { profiles -> profiles.associateBy { it.uid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val polls: StateFlow<List<AlarmPoll>> = PollRepository.observePolls(alarmId)
        .map { list -> list.filter { it.status == "open" }.sortedBy { it.endsAtMs } }
        .catch { e -> error = e.localizedMessage ?: str(R.string.error_server) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val votes: StateFlow<List<PollVote>> = polls
        .map { list -> list.map { it.id } }
        .distinctUntilChanged()
        .flatMapLatest { ids -> PollRepository.observeVotes(ids) }
        .catch { e -> error = e.localizedMessage ?: str(R.string.error_server) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedPollId by mutableStateOf<String?>(null)
        private set

    // Sélectionne l'onglet
    fun selectPoll(id: String) {
        selectedPollId = id
    }

    // Envoie mon vote
    fun vote(pollId: String, vote: Boolean) {
        if (isVoting) return
        isVoting = true
        error = null
        viewModelScope.launch {
            try {
                PollRepository.vote(pollId, vote)
            } catch (e: Exception) {
                error = e.localizedMessage ?: str(R.string.vote_error)
            } finally {
                isVoting = false
            }
        }
    }


}
