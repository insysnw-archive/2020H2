package ui

import javafx.beans.property.SimpleStringProperty
import javafx.collections.ListChangeListener
import javafx.scene.Parent
import network.Client
import tornadofx.*

class ClientView : View("Client") {
    val nicknameProperty = SimpleStringProperty("Nickname${(0..Int.MAX_VALUE).random()}")

    val client = Client(nicknameProperty.value)

    override val root: Parent = vbox(spacing = 16) {
        paddingAll = 16.0
        label("Nickname")
        hbox(8) {
            textfield().bind(nicknameProperty)
            button("Change") {
                setOnAction {
                    client.nickname = nicknameProperty.value
                    client.sendMessage("renameNickname:${client.nickname}")
                }
            }
        }
        label("Chat")
        textarea {
            prefRowCount = 24
            isEditable = false
            isWrapText = true
            client.chatStrings.addListener(ListChangeListener {
                text = it.list.joinToString("\n")
            })
        }
        hbox(8) {
            textfield() { prefWidth = 500.0 }.bind(client.newMessage)
            button("Send") {
                setOnAction {
                    client.sendMessage()
                    client.newMessage.value = ""
                }
                shortcut("Enter")
            }
        }
    }

    override fun onUndock() {
        super.onUndock()
        client.onDestroy()
    }
}