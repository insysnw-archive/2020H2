import kotlinx.css.Color
import kotlinx.css.Display
import kotlinx.css.Overflow
import kotlinx.css.Position
import kotlinx.css.backgroundColor
import kotlinx.css.display
import kotlinx.css.height
import kotlinx.css.left
import kotlinx.css.overflow
import kotlinx.css.paddingTop
import kotlinx.css.pct
import kotlinx.css.position
import kotlinx.css.px
import kotlinx.css.top
import kotlinx.css.width
import kotlinx.css.zIndex
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
import styled.css
import styled.styledDiv

external interface NameFormProps : RProps {
    var onSubmit: (String) -> Unit
}

val nameForm = functionalComponent<NameFormProps> { props ->
    val (text, setText) = useState("")
    val (isHide, hide) = useState(false)

    val submitHandler: (Event) -> Unit = {
        it.preventDefault()
        setText("")
        hide(true)
        props.onSubmit(text)
    }

    val changeHandler: (Event) -> Unit = {
        val value = (it.target as HTMLInputElement).value
        setText(value)
    }

    styledDiv {

        css {
            display = if (isHide) Display.none else Display.block
            position = Position.fixed
            width = 100.pct
            height = 100.pct
            zIndex = 1
            paddingTop = 100.px
            left = 0.px
            top = 0.px
            overflow = Overflow.auto
            backgroundColor = Color.black.withAlpha(0.8)

        }
        form(classes = "name_form") {
            +"Enter your name"
            attrs.onSubmitFunction = submitHandler
            input(InputType.text) {
                attrs.onChangeFunction = changeHandler
                attrs.value = text
            }
            button {
                +"ok"
            }
        }
    }

}

fun RBuilder.nameForm(onSubmit: (String) -> Unit): ReactElement {
    return child(nameForm) {
        attrs.onSubmit = onSubmit
    }
}