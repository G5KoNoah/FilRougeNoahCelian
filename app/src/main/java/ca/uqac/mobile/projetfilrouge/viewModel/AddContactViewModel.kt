package ca.uqac.mobile.projetfilrouge.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.data.Friend
import ca.uqac.mobile.projetfilrouge.data.FriendRepository
import ca.uqac.mobile.projetfilrouge.data.FriendStatus
import ca.uqac.mobile.projetfilrouge.data.UserSearchResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddContactViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val me: String? = Firebase.auth.currentUser?.uid

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

    private val listeners = mutableListOf<ListenerRegistration>()
    private var searchJob: Job? = null

    init {
        if (me == null) {
            error = "No user signed in"
        } else {
            listeners += db.collection("friendships")
                .whereArrayContains("members", me)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener showError(e)
                    friendUids.value = snapshot?.documents.orEmpty().mapNotNull { doc ->
                        (doc.get("members") as? List<*>)?.filterIsInstance<String>()?.firstOrNull { it != me }
                    }.toSet()
                }
            listeners += db.collection("friendRequests")
                .whereEqualTo("from", me)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener showError(e)
                    sentTo.value = snapshot?.documents.orEmpty().mapNotNull { it.getString("to") }.toSet()
                }
            listeners += db.collection("friendRequests")
                .whereEqualTo("to", me)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener showError(e)
                    receivedFrom.value = snapshot?.documents.orEmpty().mapNotNull { it.getString("from") }.toSet()
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

    private fun showError(e: Exception) {
        error = e.localizedMessage ?: "Firestore error"
    }

    override fun onCleared() {
        listeners.forEach { it.remove() }
        super.onCleared()
    }
}
