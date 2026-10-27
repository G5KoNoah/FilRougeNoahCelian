package ca.uqac.mobile.projetfilrouge.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

object FriendRepository {

    private val db get() = Firebase.firestore

    private val searchFields = listOf("firstName", "lastName", "email")

    suspend fun searchUsers(query: String, excludeUid: String): List<Friend> {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val first = words.first()
        val spellings = listOf(
            first,
            first.lowercase(),
            first.lowercase().replaceFirstChar { it.uppercase() },
            first.uppercase(),
        ).distinct()

        val documents = coroutineScope {
            searchFields.flatMap { field ->
                spellings.map { prefix ->
                    async {
                        db.collection("Account")
                            .whereGreaterThanOrEqualTo(field, prefix)
                            .whereLessThan(field, prefix + "")
                            .limit(10)
                            .get()
                            .await()
                            .documents
                    }
                }
            }.awaitAll().flatten()
        }

        return documents
            .distinctBy { it.id }
            .filter { it.id != excludeUid }
            .map { doc ->
                val account = doc.toObject(AccountData::class.java) ?: AccountData()
                Friend(doc.id, account.email, account.firstName, account.lastName)
            }
            .filter { user ->
                val fields = listOf(user.firstName, user.lastName, user.email).map { it.lowercase() }
                words.all { word -> fields.any { it.startsWith(word.lowercase()) } }
            }
            .sortedBy { it.displayName.lowercase() }
    }

    suspend fun sendRequest(fromUid: String, toUid: String) {
        val sender = db.collection("Account").document(fromUid)
            .get().await()
            .toObject(AccountData::class.java) ?: AccountData()
        db.collection("friendRequests").document(friendRequestId(fromUid, toUid))
            .set(
                mapOf(
                    "from" to fromUid,
                    "to" to toUid,
                    "fromName" to "${sender.firstName} ${sender.lastName}".trim(),
                    "fromEmail" to sender.email,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            .await()
    }

    suspend fun acceptRequest(myUid: String, fromUid: String) {
        db.batch()
            .delete(db.collection("friendRequests").document(friendRequestId(fromUid, myUid)))
            .set(
                db.collection("friendships").document(friendshipId(myUid, fromUid)),
                mapOf(
                    "members" to listOf(myUid, fromUid).sorted(),
                    "since" to FieldValue.serverTimestamp(),
                ),
            )
            .commit()
            .await()
    }

    suspend fun declineRequest(myUid: String, fromUid: String) {
        db.collection("friendRequests").document(friendRequestId(fromUid, myUid)).delete().await()
    }

    suspend fun removeFriend(myUid: String, friendUid: String) {
        db.collection("friendships").document(friendshipId(myUid, friendUid)).delete().await()
    }
}
