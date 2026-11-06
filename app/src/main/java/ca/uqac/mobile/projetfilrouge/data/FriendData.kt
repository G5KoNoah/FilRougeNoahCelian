package ca.uqac.mobile.projetfilrouge.data

// Profil d'un utilisateur
data class Friend(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null,
) {
    val displayName: String get() = displayNameOf(firstName, lastName, email)
}

// Demande d'ami reçue
data class FriendRequest(
    val id: String,
    val fromUid: String,
    val fromName: String,
    val fromEmail: String,
)

// Relation avec un utilisateur trouvé par la recherche
enum class FriendStatus { NONE, REQUEST_SENT, REQUEST_RECEIVED, FRIEND }

// Résultat de recherche avec la relation actuelle
data class UserSearchResult(
    val user: Friend,
    val status: FriendStatus,
)

// "Prénom Nom", ou l'email si le nom est vide
fun displayNameOf(firstName: String, lastName: String, email: String): String =
    "$firstName $lastName".trim().ifEmpty { email }

// Id d'une demande d'ami
fun friendRequestId(fromUid: String, toUid: String): String = "${fromUid}_$toUid"
