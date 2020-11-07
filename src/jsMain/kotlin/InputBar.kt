import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.RBuilder
import react.RProps
import react.ReactElement
import react.child
import react.dom.button
import react.dom.form
import react.dom.input
import react.functionalComponent
import react.useState

external interface InputProps : RProps {
    var onSubmit: (String) -> Unit
}

val InputBar = functionalComponent<InputProps> { props ->
    val (text, setText) = useState("")

    val submitHandler: (Event) -> Unit = {
        it.preventDefault()
        setText("")
        props.onSubmit(text)
    }

    val changeHandler: (Event) -> Unit = {
        val value = (it.target as HTMLInputElement).value
        setText(value)
    }
    form(classes = "input_bar") {
        attrs.onSubmitFunction = submitHandler
        input(InputType.text) {
            attrs.onChangeFunction = changeHandler
            attrs.value = text
        }
        button(type = ButtonType.submit) {
            +"Send"
        }
    }
}

fun RBuilder.inputBar(onSubmit: (String) -> Unit): ReactElement {
    return child(InputBar) {
        attrs.onSubmit = onSubmit
    }
}
