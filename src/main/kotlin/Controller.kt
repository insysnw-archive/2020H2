import data.clientsStorage
import data.emailStorage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import model.ERROR_CODE
import model.IndexData
import model.MailData
import model.SUCCESS_CODE
import model.ServerMessage
import model.UserNameData
import model.deletingMailSuccessMsg
import model.incorrectEmailMsg
import model.invalidRequest
import model.quitMsg
import model.successSendMailMsg
import model.welcomeMsg
import java.lang.Exception
import java.lang.IndexOutOfBoundsException
import java.net.Socket

fun login(clientSocket: Socket, buf: ByteArray) {

    val typeReq = buf.first()
    if (typeReq == 0.toByte()) {
        val emailStr = buf.getMsg()
        var emailData: UserNameData? = null
        try {
            emailData = Json.decodeFromString<UserNameData>(emailStr)
        } catch (e: Exception) {
            println(e.message)
        }
        if (emailData != null) {
            println("login new client: ${emailData.name}")
            if (EMAIL_RFC_REG.toRegex().matches(emailData.name)) {
                clientsStorage[emailData.name] = clientSocket
                emailStorage.getOrPut(emailData.name) { mutableListOf() }
                clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(welcomeMsg()))
                listenClient(clientSocket, emailData.name)
            } else {
                clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(incorrectEmailMsg()))
                listenClient(clientSocket, null)
            }
        } else {
            clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(invalidRequest()))
            listenClient(clientSocket, null)
        }
    } else {
        login(clientSocket, buf)
    }
}

fun sendMail(buf: ByteArray, clientSocket: Socket, username: String) {
    // println("sendMail")
    val emailMsgStr = buf.getMsg()
    var mailData: MailData? = null
    try {
        mailData = Json.decodeFromString<MailData>(emailMsgStr)
    } catch (e: Exception) {
        println(e.message)
    }
    if (mailData != null) {
        if (EMAIL_RFC_REG.toRegex().matches(mailData.to)) {
            mailData.from = username
            emailStorage[username]?.add(mailData)
            if (!emailStorage.containsKey(mailData.to)) {
                emailStorage[mailData.to] = mutableListOf()
            }
            emailStorage[mailData.to] = emailStorage[mailData.to].apply { this!!.add(mailData) }!!
            clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(successSendMailMsg()))
        } else {
            clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(ServerMessage("Incorrect sender email")))
        }
    } else {
        clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(invalidRequest()))
    }
}

fun readMails(clientSocket: Socket, username: String) {
    // println("readMails")
    // println(emailStorage[username])
    clientSocket.getOutputStream()
        .write(
            byteArrayOf(SUCCESS_CODE) + Json.encodeToString(
                ListSerializer(MailData.serializer()),
                emailStorage[username]!!.toList()
            ).toByteArray()
        )
}

fun quit(clientSocket: Socket, username: String) {
    println("quit user: $username")
    clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(quitMsg(username)))
    clientsStorage.remove(username)
}

fun deleteMail(clientSocket: Socket, buf: ByteArray, username: String) {
    val mailIdStr = buf.getMsg()
    var mailId: Int? = null
    try {
        mailId = Json.decodeFromString<IndexData>(mailIdStr).index
    } catch (e: Exception) {
        println(e.message)
    }
    if (mailId != null) {
        try {
            emailStorage[username]?.removeAt(mailId)
            clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(deletingMailSuccessMsg(mailId)))
        } catch (e: IndexOutOfBoundsException) {
            clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(invalidRequest()))
        }
    } else {
        clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(invalidRequest()))
    }
}


