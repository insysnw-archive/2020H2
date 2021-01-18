package model

import kotlinx.serialization.Serializable


@Serializable
data class MailData(
    val from: String? = null,
    val to: String,
    val header: String,
    val content: String,
    val time: String
)