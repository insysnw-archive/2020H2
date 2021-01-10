import data.clientsStorage
import data.emailStorage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import model.ERROR_CODE
import model.IndexData
import model.MailData
import model.SUCCESS_CODE
import model.UserNameData
import model.deletingMailSuccessMsg
import model.incorrectEmailMsg
import model.invalidRequest
import model.quitMsg
import model.successSendMailMsg
import model.welcomeMsg
import java.lang.Exception
import java.net.Socket
import kotlin.concurrent.thread

fun login(buf: ByteArray, clientSocket: Socket) {
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
            thread {
                listenClient(clientSocket, emailData.name)
            }
        } else {
            clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(incorrectEmailMsg()))
        }
    } else {
        clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(invalidRequest()))
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
        // if (clientsStorage.containsKey(mailData.to)) {
        mailData.from = username
        clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(successSendMailMsg()))
        emailStorage[username]?.add(mailData)
        emailStorage.getOrPut(mailData.to) { mutableListOf() }.add(mailData)
        // } else {
        //     clientSocket.getOutputStream()
        //         .write(ERROR_CODE.toServerResponse(getUserNotFoundMsg()))
        // }
    } else {
        clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(invalidRequest()))
    }
}

fun readMails(clientSocket: Socket, username: String) {
    // println("readMails")
    println(emailStorage[username])
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
    clientSocket.close()
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
        emailStorage[username]?.removeAt(mailId)
        clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(deletingMailSuccessMsg(mailId)))
    } else {
        clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(invalidRequest()))
    }
}


