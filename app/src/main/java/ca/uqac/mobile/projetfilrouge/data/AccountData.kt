package ca.uqac.mobile.projetfilrouge.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Ligne de la table accounts
@Serializable
data class AccountData (
    val email: String = "",
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val id: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
)
