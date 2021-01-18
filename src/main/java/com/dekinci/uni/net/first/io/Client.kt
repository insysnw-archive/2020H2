package com.dekinci.uni.net.first.io

import com.dekinci.uni.net.first.*
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.random.Random

fun main(args: Array<String>) {
    val serverString = if (args.isEmpty()) "${Random.nextInt(0, 1000)}@localhost:4269" else args[0]
    val adds = serverString.split(":", "@")

    registerMapping(Announcement::class)
    registerMapping(MessageUpdate::class)
    registerMapping(Kick::class)

    val run = AtomicBoolean(true)
    val keepConnection = AtomicBoolean(true)
    val currentConnection = AtomicReference<IoFacade>()

    val pinguin = Executors.newSingleThreadScheduledExecutor()
    pinguin.scheduleAtFixedRate({
        val connection = currentConnection.get()
        if (keepConnection.get() && connection != null) {
            try {
                connection.ping()
            } catch (e: Exception) {
                keepConnection.set(false)
            }
        }
    }, 0, 500, TimeUnit.MILLISECONDS)

    while (run.get()) {
        keepConnection.set(true)
        try {
            Socket(adds[1], adds[2].toInt()).use { socket ->
                socket.sendBufferSize = BlockingReceiver.bufferSize
                socket.receiveBufferSize = BlockingReceiver.bufferSize
                val connection = IoFacade(socket.getInputStream(), socket.getOutputStream())
                currentConnection.set(connection)

                thread {
                    try {
                        while (keepConnection.get()) {
                            when (val message = decodeMapped(connection.waitForMessage())) {
                                is MessageUpdate -> println(message)
                                is Announcement -> println(message)
                                is Kick -> {
                                    run.set(false)
                                    keepConnection.set(false)
                                    println(message)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        keepConnection.set(false)
                        System.err.println("${e.javaClass.simpleName} ${e.message}")
                    }
                }

                println("Connecting to $serverString")
                connection.writeMessage(encode(Handshake(adds[0])))

                while (keepConnection.get()) {
                    if (keepConnection.get() && System.`in`.available() > 0) {
                        var text = readLine()
                        if (text == "long")
                            text = "rdtfhmgyhu".repeat(1_000_000)
                        text?.let { connection.writeMessage(encode(Message(it))) }
                    }

                    // rate and cycle limiter XD
                    Thread.sleep(10)
                }
                currentConnection.set(null)
            }
        } catch (e: Exception) {
            currentConnection.set(null)
            keepConnection.set(false)
            System.err.println("${e.javaClass.simpleName} ${e.message}")
            Thread.sleep(1000)
        }
    }
}
