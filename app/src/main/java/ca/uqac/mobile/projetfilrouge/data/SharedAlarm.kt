package ca.uqac.mobile.projetfilrouge.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedAlarm(
    val id: String = "",
    @SerialName("owner_id") val ownerId: String = "",
    val name: String = "",
    val hour: Int = 6,
    val minute: Int = 0,
    val days: List<Int> = emptyList(),
    val ringtone: String = "",
    val snooze: String = "",
    val sound: Boolean = true,
    val vibrations: Boolean = true,
    val members: List<String> = emptyList(),
    @SerialName("disabled_by") val disabledBy: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
) {
    val timeText: String get() = "%02d:%02d".format(hour, minute)

    val requestCode: Int get() = id.hashCode()

    fun isEnabledFor(uid: String): Boolean = uid !in disabledBy
}
