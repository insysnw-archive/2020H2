import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ServerMessage

fun Byte.toServerResponse(message: ServerMessage) =
    byteArrayOf(this) + Json.encodeToString(message).toByteArray()

fun ByteArray.getMsg() = String(this.drop(1).filter { it != 0.toByte() }.toByteArray())