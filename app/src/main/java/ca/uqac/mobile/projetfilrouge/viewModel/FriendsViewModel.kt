package ca.uqac.mobile.projetfilrouge.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.data.Friend
import ca.uqac.mobile.projetfilrouge.data.FriendRequest
import ca.uqac.mobile.projetfilrouge.data.FriendRepository
import ca.uqac.mobile.projetfilrouge.data.uidChanges
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FriendsViewModel : ViewModel() {

    private var uid: String? = null

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _requests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val requests: StateFlow<List<FriendRequest>> = _requests.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // collectLatest annule les écoutes de l'ancien utilisateur à chaque connexion / déconnexion
        viewModelScope.launch {
            uidChanges.collectLatest { onUserChanged(it) }
        }
    }

    private suspend fun onUserChanged(newUid: String?) {
        uid = newUid
        _friends.value = emptyList()
        _requests.value = emptyList()
        _error.value = null

        if (newUid == null) {
            _error.value = "No user signed in"
            return
        }
        coroutineScope {
            launch {
                FriendRepository.observeFriendUids(newUid)
                    .catch { e -> _error.value = e.localizedMessage ?: "Supabase error" }
                    .collectLatest { friendUids ->
                        try {
                            _friends.value = FriendRepository.getFriends(friendUids)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            _error.value = e.localizedMessage ?: "Supabase error"
                        }
                    }
            }
            launch {
                FriendRepository.observeReceivedRequests(newUid)
                    .catch { e -> _error.value = e.localizedMessage ?: "Supabase error" }
                    .collect { _requests.value = it }
            }
        }
    }

    fun acceptRequest(request: FriendRequest) {
        val me = uid ?: return
        viewModelScope.launch {
            try {
                FriendRepository.acceptRequest(me, request.fromUid)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Couldn't accept the request"
            }
        }
    }

    fun declineRequest(request: FriendRequest) {
        val me = uid ?: return
        viewModelScope.launch {
            try {
                FriendRepository.declineRequest(me, request.fromUid)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Couldn't decline the request"
            }
        }
    }

    fun removeFriend(friend: Friend) {
        val me = uid ?: return
        viewModelScope.launch {
            try {
                FriendRepository.removeFriend(me, friend.uid)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Couldn't remove this contact"
            }
        }
    }
}
