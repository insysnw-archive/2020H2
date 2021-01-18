package com.dekinci.uni.net.first.nio

import java.net.InetSocketAddress


fun main(args: Array<String>) {
    val serverString = if (args.isEmpty()) "localhost:4269" else args[0]
    val adds = serverString.split(":")

    val server = NonBlockingServer(InetSocketAddress(adds[0], adds[1].toInt()))
    while (true)
        server.handleConnections()
}
