package ca.uqac.mobile.projetfilrouge.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object AlarmRepository {

    private val db get() = Firebase.firestore
    private val alarms get() = db.collection("alarms")

    fun observeAlarmsOf(uid: String): Flow<List<SharedAlarm>> = callbackFlow {
        val registration = alarms
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(SharedAlarm::class.java).orEmpty()
                trySend(list.sortedWith(compareBy({ it.hour }, { it.minute })))
            }
        awaitClose { registration.remove() }
    }

    fun observeAlarm(alarmId: String): Flow<SharedAlarm?> = callbackFlow {
        val registration = alarms.document(alarmId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(SharedAlarm::class.java))
            }
        awaitClose { registration.remove() }
    }

    suspend fun createAlarm(ownerId: String, alarm: SharedAlarm): String {
        val document = alarms.document()
        document.set(
            alarm.copy(
                ownerId = ownerId,
                members = (listOf(ownerId) + alarm.members).distinct(),
                disabledBy = emptyList(),
            )
        ).await()
        return document.id
    }

    suspend fun updateSettings(alarm: SharedAlarm) {
        alarms.document(alarm.id).update(
            mapOf(
                "name" to alarm.name,
                "hour" to alarm.hour,
                "minute" to alarm.minute,
                "days" to alarm.days,
                "ringtone" to alarm.ringtone,
                "snooze" to alarm.snooze,
                "sound" to alarm.sound,
                "vibrations" to alarm.vibrations,
            )
        ).await()
    }

    suspend fun setEnabled(alarmId: String, uid: String, enabled: Boolean) {
        val change = if (enabled) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        alarms.document(alarmId).update("disabledBy", change).await()
    }

    suspend fun addMember(alarmId: String, uid: String) {
        alarms.document(alarmId).update("members", FieldValue.arrayUnion(uid)).await()
    }

    suspend fun removeMember(alarmId: String, uid: String) {
        alarms.document(alarmId).update(
            mapOf(
                "members" to FieldValue.arrayRemove(uid),
                "disabledBy" to FieldValue.arrayRemove(uid),
            )
        ).await()
    }

    suspend fun deleteAlarm(alarmId: String) {
        alarms.document(alarmId).delete().await()
    }
}
