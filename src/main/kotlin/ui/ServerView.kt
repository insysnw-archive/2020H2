package ui

import javafx.collections.ListChangeListener
import javafx.scene.Parent
import network.Server
import tornadofx.*

class ServerView : View("Server") {

    val server = Server()

    override val root: Parent = vbox(16) {
        paddingAll = 16.0
        label("Chat")
        textarea {
            prefRowCount = 32
            isEditable = false
            server.messages.addListener(ListChangeListener {
                text = it.list.joinToString("\n")
            })
        }
        label("Clients")
        textarea {
            prefRowCount = 8
            isEditable = false
            isWrapText = true
            server.clientsNames.addListener(ListChangeListener {
                text = it.list.joinToString("\n")
            })
        }
    }

    override fun onUndock() {
        super.onUndock()
        server.onDestroy()
    }
}