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
import model.getIncorrectEmailMsg
import model.getQuitMsg
import model.getSuccessSendMailMsg
import model.getUserNotFoundMsg
import model.welcomeMsg
import java.net.Socket
import kotlin.concurrent.thread

fun login(buf: ByteArray, clientSocket: Socket) {
    println("login")
    val mbEmail = buf.getMsg()
    println(mbEmail)
    try {
        val email = Json.decodeFromString<UserNameData>(mbEmail)
        if (EMAIL_RFC_REG.toRegex().matches(email.name)) {
            clientSocket.getOutputStream().write(byteArrayOf(0))
            clientsStorage[email.name] = clientSocket
            if (!emailStorage.containsKey(email.name)) {
                emailStorage[email.name] = mutableListOf()
            }
            clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(welcomeMsg()))
            thread {
                handle(clientSocket, email.name)
            }
        } else {
            clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(getIncorrectEmailMsg()))
        }
        println(email)
    } catch (e: Exception) {
        println(e.message)
    }
}

fun sendMail(buf: ByteArray, clientSocket: Socket, username: String) {
    println("sendMail")
    val emailMsgStr = buf.getMsg()
    val mailData = Json.decodeFromString<MailData>(emailMsgStr)
    if (clientsStorage.containsKey(mailData.to)) {
        mailData.from = username
        clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(getSuccessSendMailMsg()))
        emailStorage[username]?.add(mailData)
        emailStorage[mailData.to]?.add(mailData)
    } else {
        clientSocket.getOutputStream()
            .write(ERROR_CODE.toServerResponse(getUserNotFoundMsg()))
    }
}

fun readMails(clientSocket: Socket, username: String) {
    println("readMails")
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
    println("quit")
    clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(getQuitMsg(username)))
    clientSocket.close()
}

fun deleteMail(buf: ByteArray, username: String) {
    val mailIndexStr = buf.getMsg()
    val mailIndex = Json.decodeFromString<IndexData>(mailIndexStr).index
    emailStorage[username]?.removeAt(mailIndex)
}


