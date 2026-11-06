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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.net.Uri
import android.provider.OpenableColumns
import ca.uqac.mobile.projetfilrouge.data.RingtoneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// État de l'écran d'édition d'une alarme
class AlarmEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    // Texte traduit depuis les ressources
    private fun str(@StringRes id: Int): String = getApplication<Application>().getString(id)
    private var original by mutableStateOf<SharedAlarm?>(null)
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

    var ringtoneUrl by mutableStateOf<String?>(null)
        private set

    var isUploadingRingtone by mutableStateOf(false)
        private set

    var sound by mutableStateOf(true)
        private set

    var vibrations by mutableStateOf(true)
        private set

    var members by mutableStateOf(listOfNotNull(myUid))
        private set

    var pollDuration by mutableStateOf<String?>(null)
        private set

    var friends by mutableStateOf<List<Friend>>(emptyList())
        private set

    var showMemberPicker by mutableStateOf(false)
        private set

    private var memberProfiles by mutableStateOf<Map<String, Friend>>(emptyMap())

    init {
        loadFriends()
        if (alarmId == null) loadMemberProfiles(members)
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

    // Remplit l'écran avec l'alarme chargée
    private fun fill(alarm: SharedAlarm) {
        ownerId = alarm.ownerId
        hour = alarm.hour
        minute = alarm.minute
        days = alarm.days.toSet()
        name = alarm.name
        ringtone = savedRingtone(alarm).ifBlank { null }
        ringtoneUrl = alarm.ringtoneUrl
        sound = alarm.sound
        vibrations = alarm.vibrations
        members = alarm.members
        original = alarm
        loadMemberProfiles(alarm.members)
    }

    private var memberProfilesJob: Job? = null

    // Écoute les profils des membres
    private fun loadMemberProfiles(uids: List<String>) {
        memberProfilesJob?.cancel()
        memberProfilesJob = viewModelScope.launch {
            FriendRepository.observeProfiles(uids)
                .catch { e -> error = e.localizedMessage ?: str(R.string.error_server) }
                .collect { profiles -> memberProfiles = profiles.associateBy { it.uid } }
        }
    }

    // Profil d'un membre
    fun profileOf(uid: String): Friend? = memberProfiles[uid] ?: friends.find { it.uid == uid }

    // Change l'heure
    fun onHourChange(value: Int) {
        hour = value
    }

    // Change les minutes
    fun onMinuteChange(value: Int) {
        minute = value
    }

    // Ajoute ou retire un jour
    fun toggleDay(day: Int) {
        days = if (day in days) days - day else days + day
    }

    // Change le nom
    fun onNameChange(value: String) {
        name = value
    }

    // Revient à la sonnerie par défaut
    fun clearRingtone() {
        ringtone = null
        ringtoneUrl = null
    }

    // Nom de la sonnerie, vide si c'est celle par défaut
    private fun savedRingtone(alarm: SharedAlarm): String =
        if (alarm.ringtoneUrl == null) "" else alarm.ringtone

    // Vérifie et envoie un MP3 ou WAV comme sonnerie
    fun uploadRingtone(uri: Uri) {
        val me = myUid ?: return
        if (isUploadingRingtone) return
        isUploadingRingtone = true
        error = null
        viewModelScope.launch {
            try {
                val resolver = getApplication<Application>().contentResolver
                val mimeType = resolver.getType(uri)
                if (RingtoneRepository.extensionFor(mimeType) == null) {
                    error = str(R.string.ringtone_error_format)
                    return@launch
                }
                val bytes = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: error(str(R.string.ringtone_error_upload))
                if (bytes.size > RingtoneRepository.MAX_SIZE_BYTES) {
                    error = str(R.string.ringtone_error_size)
                    return@launch
                }
                ringtoneUrl = RingtoneRepository.upload(me, bytes, mimeType!!)
                ringtone = displayName(uri) ?: str(R.string.ringtone_custom)
            } catch (e: Exception) {
                error = e.localizedMessage ?: str(R.string.ringtone_error_upload)
            } finally {
                isUploadingRingtone = false
            }
        }
    }

    // Nom du fichier choisi
    private fun displayName(uri: Uri): String? =
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    // Active ou coupe le son
    fun onSoundChange(value: Boolean) {
        sound = value
    }

    // Active ou coupe les vibrations
    fun onVibrationsChange(value: Boolean) {
        vibrations = value
    }

    // Choisit la durée du vote
    fun onPollDurationSelected(value: String) {
        pollDuration = value
    }

    // Charge mes contacts
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

    // Ouvre le choix des membres
    fun openMemberPicker() {
        showMemberPicker = true
    }

    // Ferme le choix des membres
    fun closeMemberPicker() {
        showMemberPicker = false
    }

    // Vrai si le membre est déjà enregistré, donc non décochable
    fun isSavedMember(uid: String): Boolean = uid == ownerId || uid in original?.members.orEmpty()

    val newMembers: List<String> get() = members - original?.members.orEmpty().toSet()

    // Coche ou décoche un nouveau membre
    fun toggleMember(uid: String) {
        if (isSavedMember(uid)) return
        members = if (uid in members) members - uid else members + uid
    }

    // Enregistre l'alarme, ou seulement les membres si elle est partagée
    fun save(onDone: () -> Unit) {
        val me = myUid ?: return
        if (needsVote) return saveMembers()
        launchSaving(str(R.string.alarm_error_save)) {
            val alarm = SharedAlarm(
                id = alarmId.orEmpty(),
                name = name.trim(),
                hour = hour,
                minute = minute,
                days = days.sorted(),
                ringtone = ringtone.orEmpty(),
                ringtoneUrl = ringtoneUrl,
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

    // Ajoute les nouveaux membres sans toucher aux réglages
    fun saveMembers() {
        val id = alarmId ?: return
        val added = newMembers
        if (added.isEmpty()) return
        launchSaving(str(R.string.alarm_error_add_member)) {
            for (uid in added) AlarmRepository.addMember(id, uid)
            original = original?.copy(members = original?.members.orEmpty() + added)
        }
    }

    // Supprime l'alarme si j'en suis propriétaire, sinon la quitte
    fun deleteOrLeave(onDone: () -> Unit) {
        val me = myUid ?: return
        val id = alarmId ?: return
        launchSaving(str(R.string.alarm_error_remove)) {
            if (isOwner) AlarmRepository.deleteAlarm(id) else AlarmRepository.removeMember(id, me)
            onDone()
        }
    }

    // Lance une sauvegarde avec indicateur de chargement et gestion d'erreur
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


    val needsVote: Boolean get() = (original?.members?.size ?: 0) > 1

    // Réglages modifiés à soumettre au vote
    fun votedChanges(): JsonObject {
        val o = original ?: return JsonObject(emptyMap())
        return buildJsonObject {
            if (hour != o.hour) put("hour", hour)
            if (minute != o.minute) put("minute", minute)
            if (days.sorted() != o.days.sorted()) put("days",
                JsonArray(days.sorted().map { JsonPrimitive(it) })
            )
            if (ringtone.orEmpty() != savedRingtone(o) || ringtoneUrl != o.ringtoneUrl) {
                put("ringtone", ringtone.orEmpty())
                put("ringtone_url", ringtoneUrl)
            }
            if (name.trim() != o.name) put("name", name.trim())
            if (sound != o.sound) put("sound", sound)
            if (vibrations != o.vibrations) put("vibrations", vibrations)
        }
    }

    val canStartPoll: Boolean
        get() = !isNew && needsVote && pollDuration != null && votedChanges().isNotEmpty()

    // Crée le vote avec les réglages modifiés
    fun startPoll(onCreated: () -> Unit) {
        val id = alarmId ?: return
        val minutes = pollDuration?.substringBefore(" ")?.toIntOrNull() ?: return
        launchSaving(str(R.string.alarm_error_poll)) {
            PollRepository.createPoll(id, votedChanges(), minutes)
            onCreated()
        }
    }

}
