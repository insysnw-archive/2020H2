package common

import com.beust.klaxon.*
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

private val klaxon = Klaxon()

class TypeConverter(private val types: Array<out MessageTypes>) : Converter {
    constructor() : this(emptyArray())

    override fun canConvert(cls: Class<*>) = true

    override fun fromJson(jv: JsonValue): MessageTypes =
            getTypeByCode(types, jv.string) ?: throw IllegalStateException()

    override fun toJson(value: Any) = "\"${(value as MessageTypes).code}\""
}

fun String.substringBeforeIncluding(delimiter: Char) = this.substringBefore(delimiter) + delimiter

fun SocketChannel.writeMessage(message: Message, print: Boolean = false) {
    if (print)
        println(message)

    val sendingString = "${klaxon.fieldConverter(Type::class, TypeConverter()).toJsonString(message)}\n"
    write(ByteBuffer.wrap(sendingString.toByteArray()))
}

@Throws(IllegalStateException::class)
fun SocketChannel.readMessage(types: Array<out MessageTypes>, print: Boolean = false): Message {
    var jsonObject: JsonObject? = null

    val receivedString = StringBuilder()

    do {
        val buffer = ByteBuffer.allocate(256)
        read(buffer)

        receivedString.append(String(buffer.array()).substringBeforeIncluding('}'))
        try {
            jsonObject = klaxon
                    .parser(Message::class)
                    .parse(receivedString) as JsonObject
            break
        } catch (e: KlaxonException) {
        }
    } while (receivedString.startsWith('{'))
    check(jsonObject != null)

    val type = getTypeByCode(types, jsonObject.string("type"))
    check(type != null)

    val message = try {
        klaxon
                .fieldConverter(Type::class, TypeConverter(types))
                .fromJsonObject(jsonObject, type.kClass.java, type.kClass)
    }
    catch (e: KlaxonException) {
        throw IllegalStateException()
    }

    return (message as Message).also { if (print) println(it) }
}
