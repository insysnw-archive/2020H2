package com.dekinci.uni.net.first.io

import java.net.InetAddress
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val serverString = if (args.isEmpty()) "localhost:4269" else args[0]
    val adds = serverString.split(":")
    val server = BlockingServer(InetSocketAddress(InetAddress.getByName(adds[0]), adds[1].toInt()))
    while (true) {
        server.handleConnection()
    }
}
