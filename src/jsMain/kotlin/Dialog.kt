import kotlinx.css.Color.Companion.white
import kotlinx.css.Overflow
import kotlinx.css.background
import kotlinx.css.flexGrow
import kotlinx.css.flexShrink
import kotlinx.css.height
import kotlinx.css.overflowY
import kotlinx.css.pct
import kotlinx.datetime.internal.JSJoda.DateTimeFormatter
import kotlinx.datetime.internal.JSJoda.LocalDateTime
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import react.dom.div
import react.dom.img
import react.dom.p
import styled.css
import styled.styledDiv

external interface ChatProps : RProps {
    var msgs: Array<MessageItem>
}

class Chat : RComponent<ChatProps, RState>() {
    override fun RBuilder.render() {
        styledDiv {
            css {
                flexGrow = 1.0
                flexShrink = 1.0
                overflowY = Overflow.auto
                height = 100.pct
                background = white.value
            }
            props.msgs.forEach { item ->
                div(classes = "chat_item") {
                    img(src = "user-circle-solid.svg") {

                    }
                    div("bubble") {
                        p("name") {
                            +item.userName
                        }
                        p("content") {
                            +item.content
                        }
                    }
                    div("time") {
                        val time = LocalDateTime.parse(item.time, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                            .format(DateTimeFormatter.ofPattern("hh:mm"))
                        +time
                    }
                }

            }
        }
    }
}

fun RBuilder.chat(msgs: Array<MessageItem>): ReactElement {
    return child(Chat::class) {
        attrs.msgs = msgs
    }
}

