package com.dekinci.uni.net.first.io

import java.net.InetAddress

fun main(args: Array<String>) {
    val serverString = if (args.isEmpty()) "localhost:4269" else args[0]
    val adds = serverString.split(":")
    val server = BlockingServer(adds[1].toInt(), InetAddress.getByName(adds[0]))
    while (true) {
        server.handleConnection()
    }
}
