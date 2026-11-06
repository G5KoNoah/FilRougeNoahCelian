package ca.uqac.mobile.projetfilrouge.viewModel

import ca.uqac.mobile.projetfilrouge.R
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import android.app.Application
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

// Contacts et demandes d'ami de l'utilisateur connecté
class FriendsViewModel(application: Application) : AndroidViewModel(application) {

    // Texte traduit depuis les ressources
    private fun str(@StringRes id: Int): String = getApplication<Application>().getString(id)

    private var uid: String? = null

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _requests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val requests: StateFlow<List<FriendRequest>> = _requests.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            uidChanges.collectLatest { onUserChanged(it) }
        }
    }

    // Relance les écoutes pour le nouvel utilisateur
    private suspend fun onUserChanged(newUid: String?) {
        uid = newUid
        _friends.value = emptyList()
        _requests.value = emptyList()
        _error.value = null

        if (newUid == null) {
            _error.value = str(R.string.error_no_user)
            return
        }
        coroutineScope {
            launch {
                FriendRepository.observeFriendUids(newUid)
                    .catch { e -> _error.value = e.localizedMessage ?: str(R.string.error_server) }
                    .collectLatest { friendUids ->
                        try {
                            FriendRepository.observeProfiles(friendUids).collect { _friends.value = it }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            _error.value = e.localizedMessage ?: str(R.string.error_server)
                        }
                    }
            }
            launch {
                FriendRepository.observeReceivedRequests(newUid)
                    .catch { e -> _error.value = e.localizedMessage ?: str(R.string.error_server) }
                    .collect { _requests.value = it }
            }
        }
    }

    // Accepte la demande
    fun acceptRequest(request: FriendRequest) {
        val me = uid ?: return
        viewModelScope.launch {
            try {
                FriendRepository.acceptRequest(me, request.fromUid)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: str(R.string.contacts_error_accept)
            }
        }
    }

    // Refuse la demande
    fun declineRequest(request: FriendRequest) {
        val me = uid ?: return
        viewModelScope.launch {
            try {
                FriendRepository.declineRequest(me, request.fromUid)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: str(R.string.contacts_error_decline)
            }
        }
    }

    // Supprime le contact
    fun removeFriend(friend: Friend) {
        val me = uid ?: return
        viewModelScope.launch {
            try {
                FriendRepository.removeFriend(me, friend.uid)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: str(R.string.contacts_error_remove)
            }
        }
    }
}
