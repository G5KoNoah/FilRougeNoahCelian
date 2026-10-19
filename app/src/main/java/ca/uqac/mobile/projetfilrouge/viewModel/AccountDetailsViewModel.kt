package ca.uqac.mobile.projetfilrouge.viewModel

import androidx.lifecycle.ViewModel
import ca.uqac.mobile.projetfilrouge.data.AccountData
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountDetailsViewModel : ViewModel() {

    val uid: String? = Firebase.auth.currentUser?.uid

    private val _account = MutableStateFlow<AccountData?>(null)
    val account: StateFlow<AccountData?> = _account.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var listener: ListenerRegistration? = null

    val db = Firebase.firestore
    init {
        if (uid == null) {
            _error.value = "Aucun utilisateur connecté"
        } else {
            listener = db.collection("Account").document(uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        _error.value = e.localizedMessage ?: "Erreur Firestore"
                        return@addSnapshotListener
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        _error.value = "Profil introuvable"
                        return@addSnapshotListener
                    }
                    _error.value = null
                    _account.value = snapshot.toObject(AccountData::class.java)
                }
        }
    }

    // Arrête d'écouter Firestore quand le ViewModel est détruit
    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}
