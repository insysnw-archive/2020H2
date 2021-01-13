package commands

import com.github.ajalt.clikt.core.CliktCommand

class ClearCommand : CliktCommand(help = "Clear console") {
    override fun run() {
        for (i in 1..100) {echo("\b")}
    }
}