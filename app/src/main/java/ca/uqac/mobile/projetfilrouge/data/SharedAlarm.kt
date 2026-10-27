package ca.uqac.mobile.projetfilrouge.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

data class SharedAlarm(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val hour: Int = 6,
    val minute: Int = 0,
    val days: List<Int> = emptyList(),
    val ringtone: String = "",
    val snooze: String = "",
    val sound: Boolean = true,
    val vibrations: Boolean = true,
    val members: List<String> = emptyList(),
    val disabledBy: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    @get:Exclude
    val timeText: String get() = "%02d:%02d".format(hour, minute)

    @get:Exclude
    val requestCode: Int get() = id.hashCode()

    fun isEnabledFor(uid: String): Boolean = uid !in disabledBy
}
