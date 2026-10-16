package ca.uqac.mobile.projetfilrouge.viewModel

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.dataStore
        by preferencesDataStore(name = "parametres")
class PreferencesManager(private val context: Context) {
    private val CLE_EST_SOMBRE =
        booleanPreferencesKey("est_sombre")
    // Lire la préférence
    val estSombre: Flow<Boolean> =
        context.dataStore.data
            .map {
                    preferences -> preferences[CLE_EST_SOMBRE] ?: false
            }
    // Écrire la préférence
    suspend fun definirThemeSombre(sombre: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLE_EST_SOMBRE] = sombre
        }
    }
}


class SettingsViewModel(private val preferencesManager: PreferencesManager
) : ViewModel() {
    val estSombre: StateFlow<Boolean> =
        preferencesManager.estSombre.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )
    fun basculerThemeSombre(sombre: Boolean) {
        viewModelScope.launch {
            preferencesManager.definirThemeSombre(sombre)
        }
    }
}