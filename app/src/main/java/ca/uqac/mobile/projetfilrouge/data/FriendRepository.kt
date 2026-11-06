package ca.uqac.mobile.projetfilrouge.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Ligne de la table friend_requests
@Serializable
private data class FriendRequestRow(
    @SerialName("from_uid") val from: String,
    @SerialName("to_uid") val to: String,
    @SerialName("from_name") val fromName: String = "",
    @SerialName("from_email") val fromEmail: String = "",
)

// Ligne de la table friendships
@Serializable
private data class FriendshipRow(
    @SerialName("user_a") val userA: String,
    @SerialName("user_b") val userB: String,
)

// Accès aux comptes, demandes d'ami et amitiés
object FriendRepository {

    private val accounts get() = supabase.from("accounts")
    private val friendRequests get() = supabase.from("friend_requests")
    private val friendships get() = supabase.from("friendships")

    // Comptes dont le prénom, le nom ou l'email commence par chaque mot recherché
    suspend fun searchUsers(query: String, excludeUid: String): List<Friend> {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

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
            .map { Friend(it.id, it.email, it.firstName, it.lastName, it.avatarUrl) }
            .filter { user ->
                val fields = listOf(user.firstName, user.lastName, user.email).map { it.lowercase() }
                words.all { word -> fields.any { it.startsWith(word.lowercase()) } }
            }
            .sortedBy { it.displayName.lowercase() }
    }

    // Profils de ces utilisateurs, triés par nom
    suspend fun getFriends(uids: List<String>): List<Friend> {
        if (uids.isEmpty()) return emptyList()
        return accounts.select { filter { isIn("id", uids) } }
            .decodeList<AccountData>()
            .map { Friend(it.id, it.email, it.firstName, it.lastName, it.avatarUrl) }
            .sortedBy { it.displayName.lowercase() }
    }

    // Profils de ces utilisateurs, mis à jour à chaque changement de accounts
    fun observeProfiles(uids: List<String>): Flow<List<Friend>> = observeTable("accounts") { getFriends(uids) }

    // Uids de mes amis, en direct
    fun observeFriendUids(myUid: String): Flow<List<String>> = observeTable("friendships") {
        friendships.select().decodeList<FriendshipRow>()
            .map { if (it.userA == myUid) it.userB else it.userA }
    }

    // Demandes d'ami reçues, en direct
    fun observeReceivedRequests(myUid: String): Flow<List<FriendRequest>> = observeTable("friend_requests") {
        friendRequests.select { filter { eq("to_uid", myUid) } }
            .decodeList<FriendRequestRow>()
            .map { FriendRequest(friendRequestId(it.from, it.to), it.from, it.fromName, it.fromEmail) }
    }

    // Uids des utilisateurs à qui j'ai envoyé une demande, en direct
    fun observeSentRequestUids(myUid: String): Flow<List<String>> = observeTable("friend_requests") {
        friendRequests.select { filter { eq("from_uid", myUid) } }
            .decodeList<FriendRequestRow>()
            .map { it.to }
    }

    // Envoie une demande d'ami
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

    // Accepte la demande et crée l'amitié (RPC)
    suspend fun acceptRequest(fromUid: String) {
        supabase.postgrest.rpc("accept_friend_request", buildJsonObject { put("p_from_uid", fromUid) })
    }

    // Refuse la demande
    suspend fun declineRequest(myUid: String, fromUid: String) {
        friendRequests.delete {
            filter {
                eq("from_uid", fromUid)
                eq("to_uid", myUid)
            }
        }
    }

    // Supprime l'amitié
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
