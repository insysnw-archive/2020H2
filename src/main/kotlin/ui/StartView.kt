package ui

import javafx.scene.Parent
import tornadofx.View
import tornadofx.button
import tornadofx.hbox
import tornadofx.paddingAll

class StartView : View("Chat") {
    override val root: Parent = hbox(spacing = 16) {
        paddingAll = 16.0
        button(text = "Client") {
            setOnAction {
                find<StartView>().replaceWith(ClientView::class, sizeToScene = true, centerOnScreen = true)
            }
        }
        button(text = "Server") {
            setOnAction {
                find<StartView>().replaceWith(ServerView::class, sizeToScene = true, centerOnScreen = true)
            }
        }
    }
}