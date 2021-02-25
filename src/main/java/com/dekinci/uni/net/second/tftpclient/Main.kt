package com.dekinci.uni.net.second.tftpclient

import java.net.InetAddress

fun main(args: Array<String>) {
    val adds = args[0].split(":")
    val command = args[1]
    val fileName = args[2]

    val client = Client(InetAddress.getByName(adds[0]), adds[1].toInt())
    when (command) {
        "read" -> client.read(fileName)
        "write" -> client.write(fileName)
        else -> println("host:port read/write filename")
    }
}