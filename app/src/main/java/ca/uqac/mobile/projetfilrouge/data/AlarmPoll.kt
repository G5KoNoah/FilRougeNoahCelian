package ca.uqac.mobile.projetfilrouge.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.collections.emptyMap

@Serializable
data class AlarmPoll(
    val id: String = "",
    @SerialName("alarm_id") val alarmId: String = "",
    @SerialName("created_by") val createdBy: String = "",
    val changes: JsonObject = JsonObject(emptyMap()),
    val previous: JsonObject = JsonObject(emptyMap()),
    val status: String = "open",
    @SerialName("required_votes") val requiredVotes: Int = 1,
    @SerialName("ends_at_ms") val endsAtMs: Long = 0,
) {
    fun isOpen(now: Long) = status == "open" && now < endsAtMs
}

@Serializable
data class PollVote(
    @SerialName("poll_id") val pollId: String = "",
    @SerialName("voter_id") val voterId: String = "",
    val vote: Boolean = false,
)