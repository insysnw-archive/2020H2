import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.TermUi.echo
import commands.*
import data.EventsRepository
import kotlin.concurrent.thread

val eventsRepository = EventsRepository()

const val debug = false

fun main(args: Array<String>) {
    LoginCommand().main(args)
    eventsRepository.registerForNotifications()
    notification()
    while (true) {
        val command = readLine() ?: ""
        if (command == "exit") {
            eventsRepository.onDestroy()
            break
        }
        try {
            MainCommand().subcommands(
                EventsListCommand(),
                AddEventCommand(),
                DeleteEventCommand(),
                SubscribeCommand(),
                UnsubscribeCommand(),
                ClearCommand()
            ).main(command.split(" "))
        } catch (e: Exception) {
            echo(e.toString())
        }
    }
}

class MainCommand : NoOpCliktCommand()

fun notification() =
    thread {
        while (true) {
            try {
                eventsRepository.notification()?.let {
                    echo("New notification: \n$it")
                }
            } catch (e: Exception) {
            }
        }
    }

fun writeLog(message: String) {
    if (debug) echo(message)
}



