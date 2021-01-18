package data

import model.Item
import java.net.Socket

val stewardStorage = mutableMapOf<String, Socket>()
val participantStorage = mutableMapOf<String, Socket>()
val itemsStorage = mutableListOf<Item>()