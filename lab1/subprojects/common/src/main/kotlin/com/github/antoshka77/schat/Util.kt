package com.github.antoshka77.schat

inline fun forever(block: () -> Unit): Nothing {
    while (true) {
        block()
    }
}
