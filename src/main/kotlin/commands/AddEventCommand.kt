package commands

import Event
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int
import eventsRepository
import utils.TimeUtil.Companion.currentTime
import utils.TimeUtil.Companion.toUnixMinutes

class AddEventCommand : CliktCommand(help = "Add new event") {
    val generate by option().flag()
    val generatePeriod by option().flag()

    override fun run() {
        val event = when {
            generate -> Event(
                name = "Loh123",
                time = currentTime() + 1,
                period = 0,
                place = "Общага",
                description = "Идем учиться в общагу",
                organizer = "Nikita"
            )
            generatePeriod -> Event(
                name = "Loh123",
                time = currentTime() + 1,
                period = 1,
                place = "Общага",
                description = "Идем учиться в общагу",
                organizer = "Nikita"
            )
            else -> {
                val enteredEvent = PromptEvent()
                enteredEvent.main(emptyList())
                with(enteredEvent) {
                    Event(
                        name = name,
                        time = time.toUnixMinutes(),
                        period = period,
                        place = place,
                        description = description,
                        organizer = organizer
                    )
                }
            }
        }
        eventsRepository.addEvent(event)
    }

    class PromptEvent : NoOpCliktCommand(help = "Add new event") {
        val name: String by option(help = "name").prompt("Enter name")
        val time: String by option(help = "time").prompt("Enter time (dd.mm.yyyy hh.mm)")
        val period: Int by option(help = "period").int().prompt("Enter period (minutes)")
        val place: String by option(help = "place").prompt("Enter place")
        val description: String by option(help = "description").prompt("Enter description")
        val organizer: String by option(help = "organizer").prompt("Enter organizer")
    }
}