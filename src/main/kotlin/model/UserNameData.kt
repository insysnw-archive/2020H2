package model

import kotlinx.serialization.Serializable

@Serializable
data class UserNameData(
    val name: String
)