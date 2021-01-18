import java.net.Socket
import java.util.Scanner
import kotlin.system.exitProcess

val maxSize = 1024

var port = 9999
var host = "127.0.0.1"
lateinit var socket: Socket

var isAuthSteward = false
var isAuthParticipant = false

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        host = args[0]
        if (args.size == 2) {
            port = args[1].toInt()
        }
    }
    socket = Socket(host, port)
    printHelp()
    listenInput()
}

fun listenInput() {
    val scanner = Scanner(System.`in`)
    printConsoleLine()
    while (scanner.hasNext()) {
        val inputString: String = scanner.nextLine()
        parseInput(inputString)
        printConsoleLine()
    }
}

fun parseInput(inputString: String) {
    val args: List<String> = inputString.split(" ")
    when (val command = args.firstOrNull()) {
        "login" -> {
            if (isCorrectLoginCommand(args)) {
                if (!isAuthSteward && !isAuthParticipant) {
                    val email = args[2]
                    val type = args[4]
                    sendLoginSteward(email, type)
                } else {
                    printConsoleLine()
                    println("You're already authorized. Print 'quit' to unauthorized")
                }
            } else {
                printIncorrectFormatError(command)
            }
        }
        "add" -> {
            if (isAuthSteward) {
                if (isCorrectAddItemCommand(args)) {
                    val toEmail = args[2]
                    val header = args[4].toInt()
                    addItem(toEmail, header)
                } else {
                    printIncorrectFormatError(command)
                }
            } else {
                printNotAuthError()
            }
        }
        "read" -> {
            if (isAuthParticipant) {
                sendReadRequest()
            } else {
                printNotAuthError()
            }
        }
        "stop" -> {
            if (isCorrectCommand(args)) {
                if (isAuthSteward) {
                    sendAuctionEndingRequest()
                } else {
                    printNotAuthError()
                }
            } else {
                printIncorrectFormatError(command)
            }
        }
        "bet" -> {
            if (isAuthParticipant) {
                if (isCorrectChangeItemCommand(args)) {
                    val name = args[2]
                    val price = args[4].toInt()
                    val owner = args[6]
                    bet(name, price, owner)
                } else {
                    printIncorrectFormatError(command)
                }
            } else {
                printNotAuthError()
            }
        }
        "quit" -> {
            if (isAuthSteward || isAuthParticipant) {
                sendQuitRequest(false)
            } else {
                printNotAuthError()
            }
        }
        "exit" ->  {
            socket.close()
            exitProcess(0)
        }
        else -> printIncorrectFormatError(command)
    }
}

fun isCorrectChangeItemCommand(args: List<String>): Boolean =
    (args.size == 7 && args[1] == "-name" && args[3] == "-price" && args[5] == "-owner")


fun isCorrectLoginCommand(args: List<String>): Boolean =
    (args.size == 5 && args[3] == "-type" && args[1] == "-name")


fun isCorrectAddItemCommand(args: List<String>): Boolean =
    (args.size == 5 && args[1] == "-name" && args[3] == "-price")

fun isCorrectCommand(args: List<String>): Boolean =
    args.size == 1
