package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int
import eventsRepository

class UnsubscribeCommand : CliktCommand(help = "Unsubscribe for event", name = "unsubscribe") {
    val id: Int by option(help = "id").int().prompt("Enter id")

    override fun run() {
        eventsRepository.unsubscribe(id)
    }
}