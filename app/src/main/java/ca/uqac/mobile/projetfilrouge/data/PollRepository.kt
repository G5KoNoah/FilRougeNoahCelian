package ca.uqac.mobile.projetfilrouge.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.collections.emptyList

// Accès aux votes des alarmes
object PollRepository {
    private val polls get() = supabase.from("alarm_polls")
    private val votes get() = supabase.from("poll_votes")

    // Propositions d'une alarme, en direct
    fun observePolls(alarmId: String): Flow<List<AlarmPoll>> = observeTable("alarm_polls") {
        polls.select { filter { eq("alarm_id", alarmId) } }.decodeList<AlarmPoll>()
    }

    // Propositions ouvertes de ces alarmes, en direct
    fun observeOpenPolls(alarmIds: List<String>): Flow<List<AlarmPoll>> = observeTable("alarm_polls") {
        if (alarmIds.isEmpty()) emptyList()
        else polls.select {
            filter {
                isIn("alarm_id", alarmIds)
                eq("status", "open")
            }
        }.decodeList<AlarmPoll>()
    }

    // Votes de ces propositions, en direct
    fun observeVotes(pollIds: List<String>): Flow<List<PollVote>> = observeTable("poll_votes") {
        if (pollIds.isEmpty()) emptyList()
        else votes.select { filter { isIn("poll_id", pollIds) } }.decodeList<PollVote>()
    }

    // Crée une proposition et renvoie son id (RPC)
    suspend fun createPoll(alarmId: String, changes: JsonObject, minutes: Int): String =
        supabase.postgrest.rpc("poll_create", buildJsonObject {
            put("p_alarm_id", alarmId)
            put("p_changes", changes)
            put("p_minutes", minutes)
        }).decodeAs<String>()

    // Vote pour ou contre ; la base applique le changement si la majorité est atteinte
    suspend fun vote(pollId: String, vote: Boolean) {
        supabase.postgrest.rpc("poll_vote", buildJsonObject {
            put("p_poll_id", pollId)
            put("p_vote", vote)
        })
    }

}