package com.handtruth.net.lab3.sevent

import com.handtruth.net.lab3.message.readMessage
import com.handtruth.net.lab3.message.writeMessage
import com.handtruth.net.lab3.options.toOptions
import com.handtruth.net.lab3.sevent.message.*
import com.handtruth.net.lab3.sevent.options.EventID
import com.handtruth.net.lab3.sevent.types.EventParameters
import com.handtruth.net.lab3.util.forever
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.*
import java.net.ConnectException
import java.net.InetSocketAddress
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun main(args: Array<String>) {
    val parser = ArgParser("sevent-client")
    val address by parser.option(ArgType.String, shortName = "a", description = "Server address").default("127.0.0.1")
    val port by parser.option(ArgType.Int, shortName = "p", description = "Server port").default(5987)
    parser.parse(args)

    printUsage()
    runBlocking {
        val socket = try {
            aSocket(ActorSelectorManager(Dispatchers.IO))
                .tcp()
                .connect(InetSocketAddress(address, port))
        } catch (e: ConnectException) {
            println("Connection failed.")
            return@runBlocking
        }
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)
        runClient(input, output)
        socket.close()
    }
}

suspend fun runClient(input: ByteReadChannel, output: ByteWriteChannel) = withContext(Dispatchers.IO) {
    val net = launch { handleServerMessages(input, output) }

    var shouldExit = false
    while (!shouldExit) {
        shouldExit = handleUserInput(output)
    }
    net.cancelAndJoin()
}

/**
 * Приём и обработка сообщений сервера.
 */
suspend fun handleServerMessages(input: ByteReadChannel, output: ByteWriteChannel) {
    forever {
        when (val message = input.readMessage()) {
            is ListedEvents -> {
                if (message.eventIds.isEmpty()) {
                    println("There are no events in the system yet.")
                } else {
                    println("Existing events:")
                    for (event in message.eventIds) {
                        output.writeMessage(GetEvent(event))
                    }
                }
            }
            is Filter -> {
                if (message.eventIds.isEmpty()) {
                    println("You have no subscriptions")
                } else {
                    println("Your subscriptions:")
                    for (event in message.eventIds) {
                        output.writeMessage(GetEvent(event))
                    }
                }
            }
            is EventInfo -> {
                println("  Event [${message.eventId}]. Description: ${message.event.description}")
            }
            is EventRegistration -> {
                println("A new event was registered with id ${message.eventId}.")
            }
            is FilterUpdated -> {
                println("Your subscriptions were successfully updated!")
            }
            is Notify -> {
                println("[${message.timestamp}] An event happened: '${message.event.description}'")
            }
            is DeletedEvents -> {
                if (message.eventIds.isEmpty()) {
                    println("No events were deleted.")
                } else {
                    println("Deleted event ids: ${message.eventIds.joinToString(", ")}")
                }
            }
            is Error -> {
                println("SERVER ERROR. ${message.errorCode.name}: ${message.message}")
            }
            else -> {
                println("An unknown message. Skipping...")
            }
        }
    }
}

/**
 * Чтение команд из stdin, их интерпретация и передача серверу
 */
fun handleUserInput(output: ByteWriteChannel): Boolean {
    val command = readLine()!!
    if (command.length < 4 && command != "ext") {
        println("Invalid command!")
        return false
    }
    when (val type = command.slice(0..2)) {
        "get" -> handleGetCommand(command.substring(4), output)
        "add" -> handleAddCommand(command.substring(4), output)
        "del" -> handleDeleteCommand(command.substring(4), output)
        "ext" -> return true
        else -> println("An unknown command type '${type}'")
    }
    return false
}

fun handleGetCommand(commandBody: String, output: ByteWriteChannel) = runBlocking {
    when (commandBody) {
        "events" -> output.writeMessage(ListEvents(toOptions()))
        "subs" -> output.writeMessage(GetFilter())
        else -> println("Invalid get command!")
    }
}

fun handleAddCommand(commandBody: String, output: ByteWriteChannel) {
    val commandParts = commandBody.split(Regex(""" (?=([^"]*"[^"]*")*[^"]*$)"""))
    if (commandParts.size < 2) {
        println("Invalid add command!")
        return
    }
    when (commandParts[0]) {
        "event" -> handleAddEventCommand(commandParts.subList(1, commandParts.size), output)
        "sub" -> handleAddSubCommand(commandParts.subList(1, commandParts.size), output)
        else -> println("Invalid add command!")
    }
}

fun handleAddEventCommand(args: List<String>, output: ByteWriteChannel) = runBlocking {
    if (args.size == 2 || args.size == 3) {
        val description = args[0].filter { it != '"' }
        val period = try {
            args[1].toInt()
        } catch (e: NumberFormatException) {
            println("Invalid add event command! Invalid repetition period.")
            return@runBlocking
        }
        val repeat = if (args.size == 2) -1 else try {
            args[2].toInt()
        } catch (e: NumberFormatException) {
            println("Invalid add event command! Invalid repetition number.")
            return@runBlocking
        }
        output.writeMessage(
            RegisterEvent(toOptions(), EventParameters(description, period.toDuration(DurationUnit.SECONDS), repeat))
        )
    } else {
        println("Invalid add event command!")
    }
}

fun handleAddSubCommand(args: List<String>, output: ByteWriteChannel) = runBlocking {
    if (args.size == 1) {
        try {
            val eventId = args[0].toInt()
            output.writeMessage(Subscribe(toOptions(EventID(eventId))))
        } catch (e: NumberFormatException) {
            println("Invalid add subscription command! Expected an event id as an argument.")
        }
    } else {
        println("Invalid add subscription command! Expected an event id as an argument.")
    }
}


fun handleDeleteCommand(commandBody: String, output: ByteWriteChannel) {
    val commandParts = commandBody.split(" ")
    if (commandParts.size != 2) {
        println("Invalid delete command!")
        return
    }
    when (commandParts[0]) {
        "event" -> handleDeleteEventCommand(commandParts.subList(1, commandParts.size), output)
        "sub" -> handleDeleteSubCommand(commandParts.subList(1, commandParts.size), output)
        else -> println("Invalid delete command!")
    }
}

fun handleDeleteEventCommand(args: List<String>, output: ByteWriteChannel) = runBlocking {
    if (args.size == 1) {
        try {
            val eventId = args[0].toInt()
            output.writeMessage(DeleteEvent(toOptions(EventID(eventId))))
        } catch (e: NumberFormatException) {
            println("Invalid delete event command! Expected an event id as an argument.")
        }
    } else {
        println("Invalid delete event command! Expected an event id as an argument.")
    }
}

fun handleDeleteSubCommand(args: List<String>, output: ByteWriteChannel) = runBlocking {
    if (args.size == 1) {
        try {
            val eventId = args[0].toInt()
            output.writeMessage(Unsubscribe(toOptions(EventID(eventId))))
        } catch (e: NumberFormatException) {
            println("Invalid delete subscription command! Expected an event id as an argument.")
        }
    } else {
        println("Invalid delete subscription command! Expected an event id as an argument.")
    }
}

fun printUsage() {
    println(
        """
 ____    ____                           __      
/\  _`\ /\  _`\                        /\ \__   
\ \,\L\_\ \ \L\_\  __  __     __    ___\ \ ,_\  
 \/_\__ \\ \  _\L /\ \/\ \  /'__`\/' _ `\ \ \/  
   /\ \L\ \ \ \L\ \ \ \_/ |/\  __//\ \/\ \ \ \_ 
   \ `\____\ \____/\ \___/ \ \____\ \_\ \_\ \__\
    \/_____/\/___/  \/__/   \/____/\/_/\/_/\/__/
    """
    )
    println("\n========================================================\n")
    println(
        "Usage:\n" +
                "\tget events \t\t\t\t\t\t Get all the existing events\n" +
                "\tget subs \t\t\t\t\t\t Get all your active subscriptions\n" +
                "\tadd event \"Event name\" \t\t\t Add a new event in the system.\n" +
                "\t    <period> [<repetitions>] \t Period is in seconds. Repetitions number is optional\n" +
                "\tadd sub <eventId> \t\t\t\t Subscribe to the event with given id\n" +
                "\tdel event <eventId> \t\t\t Delete the event with given id\n" +
                "\tdel sub <eventId> \t\t\t\t Cancel subscription to the event with given id\n" +
                "\text \t\t\t\t\t\t\t Exit\n"
    )
}