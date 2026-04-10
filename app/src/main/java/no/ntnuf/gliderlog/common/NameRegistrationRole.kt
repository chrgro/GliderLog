package no.ntnuf.gliderlog.common

data class NameRegistrationRole(
    val name: String?,
    val registration: String,
    val role: PilotType?,
    val isPlane: Boolean,
)

