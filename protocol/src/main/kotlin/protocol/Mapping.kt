package protocol

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties

interface Parser<T : Any> {
    fun parse(map: Map<String, String>): T
}

private val mappings = ConcurrentHashMap<String, KClass<*>>()

fun registerMapping(k: KClass<*>) {
    mappings[k.simpleName!!] = k
}

fun decodeMapped(message: Map<String, String>): Any {
    val parser = mappings[message["__type"]!!]?.companionObjectInstance
            ?: error("No mapping registered for type ${message["__type"]!!}")
    if (parser !is Parser<*>)
        error("No mapper")

    return parser.parse(message)
}

inline fun <reified K : Any> decode(message: Map<String, String>): K {
    val parser = K::class.companionObjectInstance
    if (parser !is Parser<*>)
        error("No mapper")

    return parser.parse(message) as K
}

inline fun <reified T : Any> encode(data: T): Map<String, String> {
    val props = T::class.memberProperties.associateBy { it.name }
    return props.keys
        .associateWith { props[it]?.get(data) }
        .entries
        .asSequence()
        .filter { it.value != null }
        .map { it.key to it.value.toString() }
        .toMap()
        .plus("__type" to T::class.simpleName!!)
}