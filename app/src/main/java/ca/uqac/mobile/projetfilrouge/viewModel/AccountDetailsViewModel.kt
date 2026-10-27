package ca.uqac.mobile.projetfilrouge.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.data.AccountData
import ca.uqac.mobile.projetfilrouge.data.currentUid
import ca.uqac.mobile.projetfilrouge.data.observeTable
import ca.uqac.mobile.projetfilrouge.data.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AccountDetailsViewModel : ViewModel() {

    val uid: String? = currentUid

    private val _account = MutableStateFlow<AccountData?>(null)
    val account: StateFlow<AccountData?> = _account.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        if (uid == null) {
            _error.value = "Aucun utilisateur connecté"
        } else {
            // L'écoute s'arrête toute seule quand le ViewModel est détruit (viewModelScope)
            viewModelScope.launch {
                observeTable("accounts") {
                    supabase.from("accounts").select { filter { eq("id", uid) } }
                        .decodeSingleOrNull<AccountData>()
                }
                    .catch { e -> _error.value = e.localizedMessage ?: "Erreur Supabase" }
                    .collect { account ->
                        if (account == null) {
                            _error.value = "Profil introuvable"
                        } else {
                            _error.value = null
                            _account.value = account
                        }
                    }
            }
        }
    }
}
