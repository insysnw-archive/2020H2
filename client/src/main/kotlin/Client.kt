package com.dekinci.uni.net.first.io

import protocol.*
import utils.BlockingReceiver
import utils.IoFacade
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    val serverString = if (args.isEmpty()) "localhost:8888" else args[0]
    val adds = serverString.split(":")

    registerMapping(IntData::class)
    registerMapping(ProductDetails::class)
    registerMapping(ProductList::class)
    registerMapping(Error::class)
    registerMapping(Info::class)

    val run = AtomicBoolean(true)
    val keepConnection = AtomicBoolean(true)
    val currentConnection = AtomicReference<IoFacade>()

    val pinguin = Executors.newSingleThreadScheduledExecutor { Thread(it).apply { isDaemon = true } }
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
            Socket(adds[0], adds[1].toInt()).use { socket ->
                socket.sendBufferSize = BlockingReceiver.bufferSize
                socket.receiveBufferSize = BlockingReceiver.bufferSize
                val connection = IoFacade(socket.getInputStream(), socket.getOutputStream())
                currentConnection.set(connection)

                thread {
                    try {
                        while (keepConnection.get()) {
                            when (val message = decodeMapped(connection.waitForMessage())) {
                                is IntData -> println(message)
                                is ProductDetails -> println(message)
                                is ProductList -> println(message.list.decodeProducts().joinToString("\n") { "${it.id}\t${it.name}\t${it.price}" })
                                is Error -> {
                                    println(message)
                                }
                                is Info -> println(message.message)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        keepConnection.set(false)
                        System.err.println("${e.javaClass.simpleName} ${e.message}")
                    }
                }

                println("Connecting to $serverString")

                while (keepConnection.get()) {
                    if (keepConnection.get() && System.`in`.available() > 0) {
                        readLine()?.let { command ->
                            val split = command.split(" ")
                            try {
                                when (split[0]) {
                                    "add" -> connection.writeMessage(encode(Add(split[1], split[2].toInt())))
                                    "get" -> connection.writeMessage(encode(Get(split[1].toInt())))
                                    "buy" -> connection.writeMessage(encode(Buy(split[1].toInt(), split[2].toInt())))
                                    "supply" -> connection.writeMessage(encode(Supply(split[1].toInt(), split[2].toInt())))
                                    "list" -> connection.writeMessage(encode(GetProducts()))
                                    else -> {
                                        println("commands: add/get/buy/supply/list")
                                    }
                                }
                            } catch (e: Exception) {
                                println("commands: add/get/buy/supply/list")
                            }
                        }
                    }

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
