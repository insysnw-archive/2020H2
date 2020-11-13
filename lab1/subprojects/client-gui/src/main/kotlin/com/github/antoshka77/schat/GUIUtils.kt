package com.github.antoshka77.schat

import javafx.scene.Node
import tornadofx.View

inline fun <R> disabled(node: Node, block: () -> R): R {
    node.isDisable = true
    try {
        return block()
    } finally {
        node.isDisable = false
    }
}

fun View.getCoroutineContext() = (app as ClientGUI).coroutineContext
