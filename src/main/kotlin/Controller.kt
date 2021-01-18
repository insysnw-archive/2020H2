import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.*
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun sendLoginSteward(name : String, type: String) {
    val emailData = User(name, type)
    val nameJson: String = Json.encodeToString(emailData)
    val resArr = byteArrayOf(0) + nameJson.toByteArray()
    socket.getOutputStream().write(resArr)
    thread { getLoginStewardResponse(emailData.type) }
}

fun getLoginStewardResponse(type: String) {
    val buf = ByteArray(maxSize)
    socket.getInputStream().read(buf)

    when (buf.firstOrNull()) {
        SUCCESS_CODE -> {
            if (type == "steward") {
                print(SUCCESS_STRING)
                isAuthSteward = true
            }
            if (type == "participant") {
                print(SUCCESS_STRING)
                isAuthParticipant = true
            }
        }
        ERROR_CODE -> print(ERROR_STRING)
    }
    processServerMsg(buf)
}

fun addItem(name: String, price: Int) {
    socket.getOutputStream().write(
        byteArrayOf(3) + Json.encodeToString(
            Item(
                name,
                price,
                null
            )
        ).toByteArray()
    )
    thread { getAddItem() }
}

fun bet(name: String, price: Int, owner: String) {
    socket.getOutputStream().write(
        byteArrayOf(1) + Json.encodeToString(
            Item(
                name,
                price,
                owner
            )
        ).toByteArray()
    )
    thread { getBet() }
}

fun getBet() {
    val buf = ByteArray(maxSize)
    socket.getInputStream().read(buf)

    when (buf.firstOrNull()) {
        SUCCESS_CODE -> {
            print(SUCCESS_STRING)
        }
        ERROR_CODE -> print(ERROR_STRING)
    }
    processServerMsg(buf)
}

fun getAddItem() {
    val buf = ByteArray(maxSize)
    socket.getInputStream().read(buf)

    when (buf.firstOrNull()) {
        SUCCESS_CODE -> {
            print(SUCCESS_STRING)
        }
        ERROR_CODE -> print(ERROR_STRING)
    }
    processServerMsg(buf)
}

fun sendReadRequest() {
    socket.getOutputStream().write(byteArrayOf(2))
    thread { getReadResponse() }
}

fun getReadResponse() {
    val buf = ByteArray(maxSize)
    socket.getInputStream().read(buf)

    when (buf.firstOrNull()) {
        SUCCESS_CODE -> {
            print(SUCCESS_STRING)
            val responseBody = Json.decodeFromString(ListSerializer(Item.serializer()), buf.getMsg())
            if (responseBody.isEmpty()) {
                println("No items")
            } else {
                println()
                responseBody.mapIndexed { index, item ->
                    println(
                        "[$index] from: ${item.name}\n" +
                            "    name: ${item.price}\n" +
                            "    header: ${item.owner}\n"
                    )
                }
            }
        }
        ERROR_CODE -> {
            print(ERROR_STRING)
            val serverMessage = Json.decodeFromString<ServerMessage>(buf.getMsg())
            println(serverMessage.message)
        }
    }
    printConsoleLine()
}

fun sendQuitRequest(isExit: Boolean) {
    socket.getOutputStream().write(byteArrayOf(5))
    thread { getQuitResponse(isExit) }
}

fun sendAuctionEndingRequest() {
    socket.getOutputStream().write(byteArrayOf(4))
    thread {
        getAuctionEndingRequest()
    }
}

fun getAuctionEndingRequest() {
    getReadResponse()
}

fun getQuitResponse(isExit: Boolean) {
    val buf = ByteArray(maxSize)
    socket.getInputStream().read(buf)
    when (buf.firstOrNull()) {
        SUCCESS_CODE -> {
            isAuthSteward = false
            isAuthParticipant = false
            print(SUCCESS_STRING)
        }
        ERROR_CODE -> print(ERROR_STRING)
    }
    processServerMsg(buf)
    if (isExit) {
        socket.close()
        exitProcess(0)
    }
}

private fun processServerMsg(buf: ByteArray) {
    val serverMessage = Json.decodeFromString<ServerMessage>(buf.getMsg())
    println(serverMessage.message)
    printConsoleLine()
}
