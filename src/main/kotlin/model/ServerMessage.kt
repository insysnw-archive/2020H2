package model

import com.sun.security.ntlm.Server
import kotlinx.serialization.Serializable

@Serializable
class ServerMessage(
    val message: String
)

fun welcomeMsg() = ServerMessage("Welcome to EgMail")

fun getIncorrectEmailMsg() = ServerMessage("Incorrect Email")

fun getSuccessAuthMsg() = ServerMessage("Success!")

fun getQuitMsg(email: String) = ServerMessage("Goodbye, $email")

fun getSuccessSendMailMsg() = ServerMessage("Email sended with Success.")

fun getUserNotFoundMsg() = ServerMessage("User not found.")