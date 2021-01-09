package model

import com.sun.security.ntlm.Server
import kotlinx.serialization.Serializable

@Serializable
class ServerMessage(
    val message: String
)

fun welcomeMsg() = ServerMessage("Welcome to EgMail")

fun incorrectEmailMsg() = ServerMessage("Incorrect Email.")

fun getSuccessAuthMsg() = ServerMessage("Success!")

fun quitMsg(email: String) = ServerMessage("Goodbye, $email")

fun deletingMailSuccessMsg(id: Int) = ServerMessage("Email with id $id deleted!")

fun successSendMailMsg() = ServerMessage("Success!")

fun invalidRequest() = ServerMessage("Invalid Request")

fun getUserNotFoundMsg() = ServerMessage("User not found.")

fun loginFirstMsg() = ServerMessage("You need to login firstly!")