import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ERROR_CODE
import model.IndexData
import model.MailData
import model.SUCCESS_CODE
import model.ServerMessage
import model.UserNameData
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun sendLogin(email: String) {
    val emailData = UserNameData(email)
    val nameJson: String = Json.encodeToString(emailData)
    val resArr = byteArrayOf(0) + nameJson.toByteArray()
    socket.getOutputStream().write(resArr)
    thread { getLoginResponse() }
}

fun getLoginResponse() {
    val buf = ByteArray(maxSize)
    socket.getInputStream().read(buf)

    when (buf.firstOrNull()) {
        SUCCESS_CODE -> {
            print(SUCCESS_STRING)
            isAuth = true
        }
        ERROR_CODE -> print(ERROR_STRING)
    }
    processServerMsg(buf)
}

fun sendMail(toEmail: String, header: String, content: String) {
    socket.getOutputStream().write(
        byteArrayOf(1) + Json.encodeToString(
            MailData(
                to = toEmail,
                header = header,
                content = content,
                time = getCurrTimeStr()
            )
        ).toByteArray()
    )
    thread { getSendMailResponse() }
}

fun getSendMailResponse() {
    val buf = ByteArray(maxSize)
    socket.getInputStream().read(buf)

    when (buf.firstOrNull()) {
        SUCCESS_CODE -> {
            isAuth = true
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
            val responseBody = Json.decodeFromString(ListSerializer(MailData.serializer()), buf.getMsg())
            if (responseBody.isEmpty()) {
                println("Mailbox is empty")
            } else {
                println()
                responseBody.mapIndexed { index, mailData ->
                    println(
                        "[$index] from: ${mailData.from}\n" +
                            "    to: ${mailData.to}\n" +
                            "    header: ${mailData.header}\n" +
                            "    content: ${mailData.content}\n" +
                            "    time: ${mailData.time}\n"
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

fun sendDeleteRequest(index: Int) {
    socket.getOutputStream().write(byteArrayOf(3) + Json.encodeToString(IndexData(index)).toByteArray())
    thread { getSimpleResponse() }
}

fun sendQuitRequest(isExit: Boolean) {
    socket.getOutputStream().write(byteArrayOf(4))
    thread { getQuitResponse(isExit) }
}

fun getQuitResponse(isExit: Boolean) {
    val buf = ByteArray(maxSize)
    socket.getInputStream().read(buf)

    when (buf.firstOrNull()) {
        SUCCESS_CODE -> {
            isAuth = false
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

fun getSimpleResponse() {
    val buf = ByteArray(maxSize)
    socket.getInputStream().read(buf)

    when (buf.firstOrNull()) {
        SUCCESS_CODE -> print(SUCCESS_STRING)
        ERROR_CODE -> print(ERROR_STRING)
    }
    processServerMsg(buf)
}

private fun processServerMsg(buf: ByteArray) {
    val serverMessage = Json.decodeFromString<ServerMessage>(buf.getMsg())
    println(serverMessage.message)
    printConsoleLine()
}
