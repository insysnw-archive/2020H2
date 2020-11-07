import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.css.Display
import kotlinx.css.FlexDirection
import kotlinx.css.JustifyContent
import kotlinx.css.TextAlign
import kotlinx.css.display
import kotlinx.css.flexDirection
import kotlinx.css.flexGrow
import kotlinx.css.height
import kotlinx.css.justifyContent
import kotlinx.css.marginLeft
import kotlinx.css.marginRight
import kotlinx.css.minHeight
import kotlinx.css.pct
import kotlinx.css.textAlign
import kotlinx.css.vh
import kotlinx.css.width
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.h1
import react.setState
import styled.css
import styled.styledDiv

private val scope = CoroutineScope(Dispatchers.Main + Job())

interface AppState : RState {
    var listMsgs: Array<MessageItem>
    var name: String
}

class App : RComponent<RProps, AppState>() {

    override fun AppState.init() {
        listMsgs = emptyArray()
        scope.launch {
            listenChat(chatCallback, receiveOutgoingChannel)
        }
    }

    private val chatCallback: (incomingMessage: MessageItem) -> Unit = {
        setState {
            listMsgs += it
        }
    }

    override fun RBuilder.render() {
        nameForm {
            state.name = it
        }

        styledDiv {

            css {
                display = Display.flex
                flexDirection = FlexDirection.column
                justifyContent = JustifyContent.stretch
                flexGrow = 1.0
                marginLeft = 10.pct
                marginRight = 10.pct
                width = 80.pct
                textAlign = TextAlign.center
                minHeight = 100.vh
                height = 100.vh
            }
            h1 {
                +"No gram chat"
            }

            chat(state.listMsgs)

            inputBar {
                try {
                    outgoingMessageChannel.offer(Pair(state.name, it))
                } catch (e: Exception) {
                    println("app outgoingMessageChannel.send: ${e.message}")
                }
            }
        }
    }
}

