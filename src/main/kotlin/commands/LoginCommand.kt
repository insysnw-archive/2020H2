package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import eventsRepository
import writeLog
import java.util.*

class LoginCommand : CliktCommand() {
    val login by option(help = "Login", names = arrayOf("-l", "-login")).prompt("Enter login")
    val password by option(
        help = "Password",
        names = arrayOf("-p", "-password")
    ).prompt("Enter password", hideInput = true)

    override fun run() {
        val credentials = Base64.getEncoder().encodeToString("$login:$password".toByteArray()) ?: ""
        eventsRepository.register(credentials)?.let {
            eventsRepository.setToken(it)
            writeLog("Token: ${it.toList()}\n")
        }
    }

}