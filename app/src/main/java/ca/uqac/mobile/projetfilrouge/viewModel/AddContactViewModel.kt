package ca.uqac.mobile.projetfilrouge.viewModel

import ca.uqac.mobile.projetfilrouge.R
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.data.Friend
import ca.uqac.mobile.projetfilrouge.data.FriendRepository
import ca.uqac.mobile.projetfilrouge.data.FriendStatus
import ca.uqac.mobile.projetfilrouge.data.UserSearchResult
import ca.uqac.mobile.projetfilrouge.data.currentUid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddContactViewModel(application: Application) : AndroidViewModel(application) {

    // Les ViewModels n'ont pas de stringResource : on passe par l'Application, qui suit la langue de l'appareil
    private fun str(@StringRes id: Int): String = getApplication<Application>().getString(id)

    private val me: String? = currentUid

    var query by mutableStateOf("")
        private set

    var isSearching by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var pendingUids by mutableStateOf<Set<String>>(emptySet())
        private set

    private val users = MutableStateFlow<List<Friend>>(emptyList())
    private val friendUids = MutableStateFlow<Set<String>>(emptySet())
    private val sentTo = MutableStateFlow<Set<String>>(emptySet())
    private val receivedFrom = MutableStateFlow<Set<String>>(emptySet())

    val results: StateFlow<List<UserSearchResult>> =
        combine(users, friendUids, sentTo, receivedFrom) { users, friends, sent, received ->
            users.map { user ->
                val status = when (user.uid) {
                    in friends -> FriendStatus.FRIEND
                    in received -> FriendStatus.REQUEST_RECEIVED
                    in sent -> FriendStatus.REQUEST_SENT
                    else -> FriendStatus.NONE
                }
                UserSearchResult(user, status)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    init {
        if (me == null) {
            error = str(R.string.error_no_user)
        } else {
            viewModelScope.launch {
                FriendRepository.observeFriendUids(me)
                    .catch { e -> showError(e) }
                    .collect { friendUids.value = it.toSet() }
            }
            viewModelScope.launch {
                FriendRepository.observeSentRequestUids(me)
                    .catch { e -> showError(e) }
                    .collect { sentTo.value = it.toSet() }
            }
            viewModelScope.launch {
                FriendRepository.observeReceivedRequests(me)
                    .catch { e -> showError(e) }
                    .collect { requests -> receivedFrom.value = requests.map { it.fromUid }.toSet() }
            }
        }
    }

    fun onQueryChange(value: String) {
        query = value
        error = null
        searchJob?.cancel()

        val text = value.trim()
        val myUid = me
        if (text.length < 2 || myUid == null) {
            users.value = emptyList()
            isSearching = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            isSearching = true
            try {
                users.value = FriendRepository.searchUsers(text, excludeUid = myUid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e)
            } finally {
                isSearching = false
            }
        }
    }

    fun sendRequest(user: Friend) = runAction(user) { myUid -> FriendRepository.sendRequest(myUid, user.uid) }

    fun acceptRequest(user: Friend) = runAction(user) { myUid -> FriendRepository.acceptRequest(myUid, user.uid) }

    private fun runAction(user: Friend, action: suspend (String) -> Unit) {
        val myUid = me ?: return
        if (user.uid in pendingUids) return
        pendingUids = pendingUids + user.uid
        viewModelScope.launch {
            try {
                action(myUid)
            } catch (e: Exception) {
                showError(e)
            } finally {
                pendingUids = pendingUids - user.uid
            }
        }
    }

    private fun showError(e: Throwable) {
        error = e.localizedMessage ?: str(R.string.error_server)
    }
}
