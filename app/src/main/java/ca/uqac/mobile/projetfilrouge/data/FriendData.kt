package ca.uqac.mobile.projetfilrouge.data

data class Friend(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
) {
    val displayName: String get() = "$firstName $lastName".trim().ifEmpty { email }
}

data class FriendRequest(
    val id: String,
    val fromUid: String,
    val fromName: String,
    val fromEmail: String,
)

enum class FriendStatus { NONE, REQUEST_SENT, REQUEST_RECEIVED, FRIEND }

data class UserSearchResult(
    val user: Friend,
    val status: FriendStatus,
)

fun friendRequestId(fromUid: String, toUid: String): String = "${fromUid}_$toUid"
