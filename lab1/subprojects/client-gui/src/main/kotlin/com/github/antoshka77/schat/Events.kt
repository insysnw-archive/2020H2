package com.github.antoshka77.schat

import tornadofx.FXEvent

class ErrorEvent(val throwable: Throwable) : FXEvent()

class MessageEvent(val message: Message) : FXEvent()

class SayEvent(val message: String) : FXEvent()
