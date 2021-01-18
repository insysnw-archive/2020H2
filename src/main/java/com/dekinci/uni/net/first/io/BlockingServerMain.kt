package com.dekinci.uni.net.first.io

fun main(args: Array<String>) {
    val port = if (args.isEmpty()) 4269 else args[0].toInt()
    val server = BlockingServer(port)
    while (true) {
        server.handleConnection()
    }
}
