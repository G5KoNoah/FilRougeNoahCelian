package ca.uqac.mobile.projetfilrouge.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.data.AccountData
import ca.uqac.mobile.projetfilrouge.data.Friend
import ca.uqac.mobile.projetfilrouge.data.FriendRequest
import ca.uqac.mobile.projetfilrouge.data.FriendRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendsViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var uid: String? = null

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _requests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val requests: StateFlow<List<FriendRequest>> = _requests.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var friendsListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null
    private var loadFriendsJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { onUserChanged(it.currentUser?.uid) }

    init {
        auth.addAuthStateListener(authListener)
    }

    private fun onUserChanged(newUid: String?) {
        if (newUid == uid && newUid != null) return

        friendsListener?.remove()
        requestsListener?.remove()
        loadFriendsJob?.cancel()
        friendsListener = null
        requestsListener = null
        uid = newUid
        _friends.value = emptyList()
        _requests.value = emptyList()
        _error.value = null

        if (newUid == null) {
            _error.value = "No user signed in"
        } else {
            friendsListener = db.collection("friendships")
                .whereArrayContains("members", newUid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        _error.value = e.localizedMessage ?: "Firestore error"
                        return@addSnapshotListener
                    }
                    val friendUids = snapshot?.documents.orEmpty().mapNotNull { doc ->
                        (doc.get("members") as? List<*>)
                            ?.filterIsInstance<String>()
                            ?.firstOrNull { it != newUid }
                    }
                    loadFriends(friendUids)
                }

            requestsListener = db.collection("friendRequests")
                .whereEqualTo("to", newUid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        _error.value = e.localizedMessage ?: "Firestore error"
                        return@addSnapshotListener
                    }
                    _requests.value = snapshot?.documents.orEmpty().map { doc ->
                        FriendRequest(
                            id = doc.id,
                            fromUid = doc.getString("from").orEmpty(),
                            fromName = doc.getString("fromName").orEmpty(),
                            fromEmail = doc.getString("fromEmail").orEmpty(),
                        )
                    }
                }
        }
    }

    private fun loadFriends(friendUids: List<String>) {
        loadFriendsJob?.cancel()
        loadFriendsJob = viewModelScope.launch {
            try {
                _friends.value = friendUids.chunked(30)
                    .flatMap { chunk ->
                        db.collection("Account")
                            .whereIn(FieldPath.documentId(), chunk)
                            .get()
                            .await()
                            .documents
                    }
                    .map { doc ->
                        val account = doc.toObject(AccountData::class.java) ?: AccountData()
                        Friend(doc.id, account.email, account.firstName, account.lastName)
                    }
                    .sortedBy { it.displayName.lowercase() }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Firestore error"
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

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        friendsListener?.remove()
        requestsListener?.remove()
        super.onCleared()
    }
}
