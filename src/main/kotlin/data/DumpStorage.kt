package data

import model.MailData
import java.net.Socket

val clientsStorage = mutableMapOf<String, Socket>()
val emailStorage = mutableMapOf<String, MutableList<MailData>>()