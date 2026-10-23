package ca.uqac.mobile.projetfilrouge.data

data class AlarmData(
    val id: Int = 0,
    val name: String = "",
    val hour: Int = 6,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val ringtone: String = "",
)
