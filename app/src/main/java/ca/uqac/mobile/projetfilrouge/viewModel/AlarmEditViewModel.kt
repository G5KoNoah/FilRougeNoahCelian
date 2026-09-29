package ca.uqac.mobile.projetfilrouge.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AlarmEditViewModel : ViewModel() {

    val ringtones = listOf("Classique", "Radar", "Carillon", "Réveil doux")
    val snoozeOptions = listOf("1 x 5 min", "2 x 3 min", "3 x 5 min")
    val pollDurations = listOf("2 min", "5 min", "10 min")

    var name by mutableStateOf("")
        private set

    var ringtone by mutableStateOf<String?>(null)
        private set

    var snooze by mutableStateOf("2 x 3 min")
        private set

    var pollDuration by mutableStateOf<String?>(null)
        private set

    fun onNameChange(value: String) {
        name = value
    }

    fun onRingtoneSelected(value: String) {
        ringtone = value
    }

    fun onSnoozeSelected(value: String) {
        snooze = value
    }

    fun onPollDurationSelected(value: String) {
        pollDuration = value
    }
}