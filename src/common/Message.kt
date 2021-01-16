package common

import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

@Target(AnnotationTarget.FIELD)
annotation class Type

interface MessageTypes {
    val code: Int
    val kClass: KClass<out Message>
}

open class Message(
        @Type var type: MessageTypes
) : Serializable {
    override fun toString(): String =
            this::class.memberProperties.joinToString(prefix = "${this::class.simpleName}(", postfix = ")") { prop ->
                "${prop.name} = ${prop.getter.call(this)}"
            }
}

class TextMessage(type: MessageTypes, val text: String) : Message(type)

fun getTypeByCode(types: Array<out MessageTypes>, code: String?) = code?.toIntOrNull()?.let { messageCode ->
    types.find { it.code == messageCode }
}
