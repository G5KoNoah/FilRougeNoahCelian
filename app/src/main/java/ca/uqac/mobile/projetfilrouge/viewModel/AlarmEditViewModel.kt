package ca.uqac.mobile.projetfilrouge.viewModel
import ca.uqac.mobile.projetfilrouge.R
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.data.AlarmRepository
import ca.uqac.mobile.projetfilrouge.data.Friend
import ca.uqac.mobile.projetfilrouge.data.FriendRepository
import ca.uqac.mobile.projetfilrouge.data.PollRepository
import ca.uqac.mobile.projetfilrouge.data.SharedAlarm
import ca.uqac.mobile.projetfilrouge.data.currentUid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AlarmEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    // Les ViewModels n'ont pas de stringResource : on passe par l'Application, qui suit la langue de l'appareil
    private fun str(@StringRes id: Int): String = getApplication<Application>().getString(id)
    // l'alarme telle qu'elle était à l'ouverture de l'écran
    // (état Compose : needsVote et canStartPoll se mettent à jour à l'écran quand l'alarme est chargée)
    private var original by mutableStateOf<SharedAlarm?>(null)
    val ringtones = listOf("Classique", "Radar", "Carillon", "Réveil doux")
    val pollDurations = listOf("2 min", "5 min", "10 min")

    val alarmId: String? = savedStateHandle.get<String>("alarmId")
    val isNew: Boolean = alarmId == null
    private val myUid: String? = currentUid

    var isLoading by mutableStateOf(!isNew)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var ownerId by mutableStateOf(myUid.orEmpty())
        private set

    val isOwner: Boolean get() = ownerId == myUid

    var hour by mutableIntStateOf(7)
        private set

    var minute by mutableIntStateOf(0)
        private set

    var days by mutableStateOf<Set<Int>>(emptySet())
        private set

    var name by mutableStateOf("")
        private set

    var ringtone by mutableStateOf<String?>(null)
        private set

    var sound by mutableStateOf(true)
        private set

    var vibrations by mutableStateOf(true)
        private set

    var members by mutableStateOf(listOfNotNull(myUid))
        private set

    var pollDuration by mutableStateOf<String?>(null)
        private set

    // Mes contacts : ce sont les seules personnes qu'on peut ajouter à l'alarme
    var friends by mutableStateOf<List<Friend>>(emptyList())
        private set

    var showMemberPicker by mutableStateOf(false)
        private set

    init {
        loadFriends()
        if (alarmId != null) {
            viewModelScope.launch {
                try {
                    val alarm = AlarmRepository.observeAlarm(alarmId).first()
                    if (alarm == null) error = str(R.string.alarm_error_not_found) else fill(alarm)
                } catch (e: Exception) {
                    error = e.localizedMessage ?: str(R.string.alarm_error_load)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    private fun fill(alarm: SharedAlarm) {
        ownerId = alarm.ownerId
        hour = alarm.hour
        minute = alarm.minute
        days = alarm.days.toSet()
        name = alarm.name
        ringtone = alarm.ringtone.ifBlank { null }
        sound = alarm.sound
        vibrations = alarm.vibrations
        members = alarm.members
        original = alarm
    }

    fun onHourChange(value: Int) {
        hour = value
    }

    fun onMinuteChange(value: Int) {
        minute = value
    }

    fun toggleDay(day: Int) {
        days = if (day in days) days - day else days + day
    }

    fun onNameChange(value: String) {
        name = value
    }

    fun onRingtoneSelected(value: String) {
        ringtone = value
    }

    fun onSoundChange(value: Boolean) {
        sound = value
    }

    fun onVibrationsChange(value: Boolean) {
        vibrations = value
    }

    fun onPollDurationSelected(value: String) {
        pollDuration = value
    }

    private fun loadFriends() {
        val me = myUid ?: return
        viewModelScope.launch {
            try {
                friends = FriendRepository.getFriends(FriendRepository.observeFriendUids(me).first())
            } catch (e: Exception) {
                error = e.localizedMessage ?: str(R.string.error_server)
            }
        }
    }

    fun openMemberPicker() {
        showMemberPicker = true
    }

    fun closeMemberPicker() {
        showMemberPicker = false
    }

    // Les membres déjà enregistrés ne se retirent pas ici (c'est "Quitter" / "Supprimer") :
    // on ne coche / décoche que les nouveaux, qui seront ajoutés au Save
    fun isSavedMember(uid: String): Boolean = uid == ownerId || uid in original?.members.orEmpty()

    // Membres cochés mais pas encore enregistrés
    val newMembers: List<String> get() = members - original?.members.orEmpty().toSet()

    fun toggleMember(uid: String) {
        if (isSavedMember(uid)) return
        members = if (uid in members) members - uid else members + uid
    }

    // Alarme seule (ou nouvelle) : on enregistre directement tous les réglages
    fun save(onDone: () -> Unit) {
        val me = myUid ?: return
        // Alarme partagée : les réglages passent par un vote, Save n'ajoute que les membres
        if (needsVote) return saveMembers()
        launchSaving(str(R.string.alarm_error_save)) {
            val alarm = SharedAlarm(
                id = alarmId.orEmpty(),
                name = name.trim(),
                hour = hour,
                minute = minute,
                days = days.sorted(),
                ringtone = ringtone.orEmpty(),
                sound = sound,
                vibrations = vibrations,
                members = members,
            )
            if (alarmId == null) {
                AlarmRepository.createAlarm(me, alarm)
            } else {
                AlarmRepository.updateSettings(alarm)
                for (uid in newMembers) AlarmRepository.addMember(alarmId, uid)
            }
            onDone()
        }
    }

    // Alarme partagée : ajoute les membres cochés, sans toucher aux réglages (qui se votent)
    fun saveMembers() {
        val id = alarmId ?: return
        val added = newMembers
        if (added.isEmpty()) return
        launchSaving(str(R.string.alarm_error_add_member)) {
            for (uid in added) AlarmRepository.addMember(id, uid)
            original = original?.copy(members = original?.members.orEmpty() + added)
        }
    }

    fun deleteOrLeave(onDone: () -> Unit) {
        val me = myUid ?: return
        val id = alarmId ?: return
        launchSaving(str(R.string.alarm_error_remove)) {
            if (isOwner) AlarmRepository.deleteAlarm(id) else AlarmRepository.removeMember(id, me)
            onDone()
        }
    }

    private fun launchSaving(errorMessage: String, block: suspend () -> Unit) {
        if (isSaving) return
        isSaving = true
        error = null
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                error = e.localizedMessage ?: errorMessage
            } finally {
                isSaving = false
            }
        }
    }


    // Une alarme à plusieurs membres demande un vote pour tous ses réglages
    val needsVote: Boolean get() = (original?.members?.size ?: 0) > 1

    // Seulement les réglages votables qui ont changé
    fun votedChanges(): JsonObject {
        val o = original ?: return JsonObject(emptyMap())
        return buildJsonObject {
            if (hour != o.hour) put("hour", hour)
            if (minute != o.minute) put("minute", minute)
            if (days.sorted() != o.days.sorted()) put("days",
                JsonArray(days.sorted().map { JsonPrimitive(it) })
            )
            if (ringtone.orEmpty() != o.ringtone) put("ringtone", ringtone.orEmpty())
            if (name.trim() != o.name) put("name", name.trim())
            if (sound != o.sound) put("sound", sound)
            if (vibrations != o.vibrations) put("vibrations", vibrations)
        }
    }

    // Le bouton "Start poll" n'est actif que s'il y a quelque chose à voter et une durée choisie
    val canStartPoll: Boolean
        get() = !isNew && needsVote && pollDuration != null && votedChanges().isNotEmpty()

    fun startPoll(onCreated: () -> Unit) {
        val id = alarmId ?: return
        val minutes = pollDuration?.substringBefore(" ")?.toIntOrNull() ?: return   // "5 min" -> 5
        launchSaving(str(R.string.alarm_error_poll)) {
            PollRepository.createPoll(id, votedChanges(), minutes)
            onCreated()
        }
    }

}
