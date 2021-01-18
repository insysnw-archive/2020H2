package model

import kotlinx.serialization.Serializable

@Serializable
data class Item(
        val name: String,
        val price: Int,
        val owner: String?
)