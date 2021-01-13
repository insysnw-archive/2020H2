package commands

import com.github.ajalt.clikt.core.CliktCommand
import eventsRepository

class EventsListCommand : CliktCommand(help = "Get list of user's events") {
    override fun run() {
        eventsRepository.getEventsList()?.let {
            if (it.isEmpty()) echo("Empty list")
            else echo(it)
        }
    }
}