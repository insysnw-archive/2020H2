package model

import kotlinx.serialization.Serializable

@Serializable
data class MailData(
    var from: String? = null,
    val to: String,
    val header: String,
    val content: String,
    val time: String
)

