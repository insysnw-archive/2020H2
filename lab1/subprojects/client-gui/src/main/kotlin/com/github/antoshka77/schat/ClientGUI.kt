package com.github.antoshka77.schat

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.layout.Priority
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tornadofx.*
import java.net.URI
import java.net.URISyntaxException

class ClientGUI : App(LoginView::class), CoroutineScope {
    override val coroutineContext = Dispatchers.JavaFx + SupervisorJob()

    override fun stop() {
        coroutineContext.cancel()
        super.stop()
    }
}

abstract class CoroutineView(title: String) : View(title), CoroutineScope {
    override val coroutineContext = getCoroutineContext()
}

class LoginView : CoroutineView("Login") {
    private val viewModel: LoginViewModel by inject()

    override val root = vbox(5) {
        form {
            fieldset("Connection string") {
                textfield(viewModel.uri) {
                    promptText = "[username@]host[:port]"
                    validator {
                        try {
                            val link = URI("tcp://$it")
                            if (link.host == null) {
                                error("host must be specified")
                            } else {
                                null
                            }
                        } catch (e: URISyntaxException) {
                            error(e.message)
                        }
                    }
                }
            }
        }
        buttonbar {
            button("Connect", ButtonBar.ButtonData.OK_DONE) {
                enableWhen(viewModel.valid)
                isDefaultButton = true
                action {
                    disabled(this@buttonbar) {
                        viewModel.commit()
                        val item = viewModel.item
                        val uri = URI("tcp://${item.uri}")
                        val nick = uri.userInfo
                        val host = uri.host!!
                        val port = uri.port.let { if (it == -1) DEFAULT_PORT else it }
                        val chat = find<ChatView>()
                        chat.openWindow(owner = null)
                        chat.process(nick, host, port)
                        close()
                    }
                }
            }
            button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                isCancelButton = true
                action {
                    close()
                }
            }
        }
        style {
            padding = box(10.px)
        }
    }
}

fun CoroutineView.process(nick: String?, host: String, port: Int) = launch {
    val messages = Channel<Message>()
    val parcels = Channel<Say>()
    launch {
        for (message in messages) {
            fire(MessageEvent(message))
        }
    }
    subscribe<SayEvent> {
        launch {
            parcels.send(Say(it.message))
        }
    }
    try {
        client(parcels, messages, host, port, nick)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        fire(ErrorEvent(e))
    }
}

class ChatView : CoroutineView("Chat") {

    private val outgoing = SimpleStringProperty("")

    override val root = borderpane {
        center = textarea {
            isEditable = false
            subscribe<MessageEvent> {
                val message = it.message
                val time = message.time.toLocalDateTime(TimeZone.currentSystemDefault())
                appendText("<${time.hour}:${time.minute}> [${message.nick}] ${message.message}\n")
            }
        }
        bottom = hbox {
            textfield(outgoing) {
                hgrow = Priority.ALWAYS
            }
            button("Send") {
                isDefaultButton = true
                action {
                    val value = outgoing.value
                    outgoing.value = ""
                    fire(SayEvent(value))
                }
            }
        }
    }

    init {
        subscribe<ErrorEvent> { error ->
            alert(Alert.AlertType.ERROR, "Error!", error.throwable.stackTraceToString(), ButtonType.FINISH) {
                this@ChatView.close()
            }
        }
    }
}
