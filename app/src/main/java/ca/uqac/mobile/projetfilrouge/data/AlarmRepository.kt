package ca.uqac.mobile.projetfilrouge.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

// Accès à la table alarms
object AlarmRepository {

    private val alarms get() = supabase.from("alarms")

    private val json = Json { encodeDefaults = true }

    // Alarmes dont l'utilisateur est membre, triées par heure, en direct
    fun observeAlarmsOf(uid: String): Flow<List<SharedAlarm>> = observeTable("alarms") {
        alarms.select { filter { contains("members", listOf(uid)) } }
            .decodeList<SharedAlarm>()
            .sortedWith(compareBy({ it.hour }, { it.minute }, { it.createdAt }, { it.id }))
    }

    // Une alarme, en direct
    fun observeAlarm(alarmId: String): Flow<SharedAlarm?> = observeTable("alarms") {
        alarms.select { filter { eq("id", alarmId) } }.decodeSingleOrNull<SharedAlarm>()
    }

    // Crée l'alarme avec son propriétaire et renvoie son id
    suspend fun createAlarm(ownerId: String, alarm: SharedAlarm): String {
        val row = json.encodeToJsonElement(
            SharedAlarm.serializer(),
            alarm.copy(
                ownerId = ownerId,
                members = (listOf(ownerId) + alarm.members).distinct(),
                disabledBy = emptyList(),
            )
        ).jsonObject
        val created = alarms.insert(JsonObject(row - "id" - "created_at")) { select() }
            .decodeSingle<SharedAlarm>()
        return created.id
    }

    // Met à jour les réglages de l'alarme
    suspend fun updateSettings(alarm: SharedAlarm) {
        alarms.update({
            set("name", alarm.name)
            set("hour", alarm.hour)
            set("minute", alarm.minute)
            set("days", alarm.days)
            set("ringtone", alarm.ringtone)
            set("ringtone_url", alarm.ringtoneUrl)
            set("sound", alarm.sound)
            set("vibrations", alarm.vibrations)
        }) { filter { eq("id", alarm.id) } }
    }

    // Active ou désactive l'alarme pour un membre (RPC)
    suspend fun setEnabled(alarmId: String, uid: String, enabled: Boolean) {
        supabase.postgrest.rpc("alarm_set_enabled", buildJsonObject {
            put("p_alarm_id", alarmId)
            put("p_uid", uid)
            put("p_enabled", enabled)
        })
    }

    // Ajoute un membre à l'alarme (RPC)
    suspend fun addMember(alarmId: String, uid: String) {
        supabase.postgrest.rpc("alarm_add_member", buildJsonObject {
            put("p_alarm_id", alarmId)
            put("p_uid", uid)
        })
    }

    // Retire un membre de l'alarme (RPC)
    suspend fun removeMember(alarmId: String, uid: String) {
        supabase.postgrest.rpc("alarm_remove_member", buildJsonObject {
            put("p_alarm_id", alarmId)
            put("p_uid", uid)
        })
    }

    // Supprime l'alarme
    suspend fun deleteAlarm(alarmId: String) {
        alarms.delete { filter { eq("id", alarmId) } }
    }
}
