package ca.uqac.mobile.projetfilrouge.viewModel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
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
                    preferences -> preferences[CLE_EST_SOMBRE] ?: true
            }
    // Écrire la préférence
    suspend fun definirThemeSombre(sombre: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLE_EST_SOMBRE] = sombre
        }
    }
}


// AndroidViewModel : la factory par défaut sait lui passer l'Application, pas besoin d'en écrire une
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)

    val estSombre: StateFlow<Boolean> =
        preferencesManager.estSombre.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )
    fun basculerThemeSombre(sombre: Boolean) {
        viewModelScope.launch {
            preferencesManager.definirThemeSombre(sombre)
        }
    }
}