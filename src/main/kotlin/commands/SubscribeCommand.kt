package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int
import eventsRepository

class SubscribeCommand : CliktCommand(help = "Subscribe for event", name = "subscribe") {
    val id: Int by option(help = "id").int().prompt("Enter id")

    override fun run() {
        eventsRepository.subscribe(id)
    }
}