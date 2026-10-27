package ca.uqac.mobile.projetfilrouge.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class FriendRequestRow(
    @SerialName("from_uid") val from: String,
    @SerialName("to_uid") val to: String,
    @SerialName("from_name") val fromName: String = "",
    @SerialName("from_email") val fromEmail: String = "",
)

@Serializable
private data class FriendshipRow(
    @SerialName("user_a") val userA: String,
    @SerialName("user_b") val userB: String,
)

object FriendRepository {

    private val accounts get() = supabase.from("accounts")
    private val friendRequests get() = supabase.from("friend_requests")
    private val friendships get() = supabase.from("friendships")

    suspend fun searchUsers(query: String, excludeUid: String): List<Friend> {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        // ilike est insensible à la casse ; on retire les caractères spéciaux de PostgREST / LIKE
        val prefix = words.first().replace(Regex("""[,()%_*\\"]"""), "")
        if (prefix.isEmpty()) return emptyList()

        val found = accounts.select {
            filter {
                neq("id", excludeUid)
                or {
                    ilike("first_name", "$prefix%")
                    ilike("last_name", "$prefix%")
                    ilike("email", "$prefix%")
                }
            }
            limit(30)
        }.decodeList<AccountData>()

        return found
            .map { Friend(it.id, it.email, it.firstName, it.lastName) }
            .filter { user ->
                val fields = listOf(user.firstName, user.lastName, user.email).map { it.lowercase() }
                words.all { word -> fields.any { it.startsWith(word.lowercase()) } }
            }
            .sortedBy { it.displayName.lowercase() }
    }

    suspend fun getFriends(uids: List<String>): List<Friend> {
        if (uids.isEmpty()) return emptyList()
        return accounts.select { filter { isIn("id", uids) } }
            .decodeList<AccountData>()
            .map { Friend(it.id, it.email, it.firstName, it.lastName) }
            .sortedBy { it.displayName.lowercase() }
    }

    // RLS : on ne reçoit que les amitiés dont on fait partie
    fun observeFriendUids(myUid: String): Flow<List<String>> = observeTable("friendships") {
        friendships.select().decodeList<FriendshipRow>()
            .map { if (it.userA == myUid) it.userB else it.userA }
    }

    fun observeReceivedRequests(myUid: String): Flow<List<FriendRequest>> = observeTable("friend_requests") {
        friendRequests.select { filter { eq("to_uid", myUid) } }
            .decodeList<FriendRequestRow>()
            .map { FriendRequest(friendRequestId(it.from, it.to), it.from, it.fromName, it.fromEmail) }
    }

    fun observeSentRequestUids(myUid: String): Flow<List<String>> = observeTable("friend_requests") {
        friendRequests.select { filter { eq("from_uid", myUid) } }
            .decodeList<FriendRequestRow>()
            .map { it.to }
    }

    suspend fun sendRequest(fromUid: String, toUid: String) {
        val sender = accounts.select { filter { eq("id", fromUid) } }
            .decodeSingleOrNull<AccountData>() ?: AccountData()
        friendRequests.upsert(
            FriendRequestRow(
                from = fromUid,
                to = toUid,
                fromName = "${sender.firstName} ${sender.lastName}".trim(),
                fromEmail = sender.email,
            )
        )
    }

    // Supprime la demande et crée l'amitié dans une seule transaction (fonction SQL)
    suspend fun acceptRequest(myUid: String, fromUid: String) {
        supabase.postgrest.rpc("accept_friend_request", buildJsonObject { put("p_from_uid", fromUid) })
    }

    suspend fun declineRequest(myUid: String, fromUid: String) {
        friendRequests.delete {
            filter {
                eq("from_uid", fromUid)
                eq("to_uid", myUid)
            }
        }
    }

    suspend fun removeFriend(myUid: String, friendUid: String) {
        val (a, b) = listOf(myUid, friendUid).sorted()
        friendships.delete {
            filter {
                eq("user_a", a)
                eq("user_b", b)
            }
        }
    }
}
