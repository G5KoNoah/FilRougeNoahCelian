package ca.uqac.mobile.projetfilrouge.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.collections.emptyList

//gestion des datas pour les votes
object PollRepository {
    private val polls get() = supabase.from("alarm_polls")
    private val votes get() = supabase.from("poll_votes")

    fun observePolls(alarmId: String): Flow<List<AlarmPoll>> = observeTable("alarm_polls") {
        polls.select { filter { eq("alarm_id", alarmId) } }.decodeList<AlarmPoll>()
    }

    // Votes encore ouverts sur ces alarmes (ceux dont le temps est écoulé sont filtrés avec isOpen(now))
    fun observeOpenPolls(alarmIds: List<String>): Flow<List<AlarmPoll>> = observeTable("alarm_polls") {
        if (alarmIds.isEmpty()) emptyList()
        else polls.select {
            filter {
                isIn("alarm_id", alarmIds)
                eq("status", "open")
            }
        }.decodeList<AlarmPoll>()
    }

    fun observeVotes(pollIds: List<String>): Flow<List<PollVote>> = observeTable("poll_votes") {
        if (pollIds.isEmpty()) emptyList()
        else votes.select { filter { isIn("poll_id", pollIds) } }.decodeList<PollVote>()
    }

    suspend fun createPoll(alarmId: String, changes: JsonObject, minutes: Int): String =
        supabase.postgrest.rpc("poll_create", buildJsonObject {
            put("p_alarm_id", alarmId)
            put("p_changes", changes)
            put("p_minutes", minutes)
        }).decodeAs<String>()

    suspend fun vote(pollId: String, vote: Boolean) {
        supabase.postgrest.rpc("poll_vote", buildJsonObject {
            put("p_poll_id", pollId)
            put("p_vote", vote)
        })
    }

}