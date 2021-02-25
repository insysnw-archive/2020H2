package com.dekinci.uni.net.second.tftpclient

import org.apache.commons.net.tftp.TFTPServer
import java.io.File
import java.io.InputStreamReader

fun main(args: Array<String>) {
    val serverString = if (args.size == 1) "0.0.0.0:6669" else args[0]
    val adds = serverString.split(":")

    val file = if (args.size == 1) File(args[0]) else File(args[1])

    val ts = TFTPServer(file, file, adds[1].toInt(), TFTPServer.ServerMode.GET_AND_PUT, System.out, System.err)
    ts.setSocketTimeout(2000)
    println("TFTP Server running.  Press enter to stop.")
    InputStreamReader(System.`in`).read()
    ts.shutdown()
    println("Server shut down.")
    System.exit(0)
}
