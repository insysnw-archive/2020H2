import java.net.Socket
import java.util.Scanner
import kotlin.system.exitProcess

val maxSize = 1024

var port = 9999
var host = "127.0.0.1"
lateinit var socket: Socket

var isAuth = false

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
                if (!isAuth) {
                    val email = args[1]
                    sendLogin(email)
                } else {
                    printConsoleLine()
                    println("You're already authorized. Print 'quit' to unauthorized")
                }
            } else {
                printIncorrectFormatError(command)
            }
        }
        "send" -> {
            if (isAuth) {
                if (isCorrectMailCommand(args)) {
                    val toEmail = args[2]
                    val header = args[4]
                    val content = args.slice(6 until args.size).joinToString(separator = " ")
                    sendMail(toEmail, header, content)
                } else {
                    printIncorrectFormatError(command)
                }
            } else {
                printNotAuthError()
            }
        }
        "read" -> {
            if (isAuth) {
                sendReadRequest()
            } else {
                printNotAuthError()
            }
        }
        "delete" -> {
            if (isCorrectDeleteCommand(args)) {
                if (isAuth) {
                    val index = args[1]
                    sendDeleteRequest(index.toInt())
                } else {
                    printNotAuthError()
                }
            } else {
                printIncorrectFormatError(command)
            }
        }
        "quit" -> {
            if (isAuth) {
                sendQuitRequest(false)
            } else {
                printNotAuthError()
            }
        }
        "help" -> printHelp()
        "exit" -> if (isAuth) {
            sendQuitRequest(true)
        } else {
            socket.close()
            exitProcess(0)
        }
        else -> printIncorrectFormatError(command)
    }
}

fun isCorrectLoginCommand(args: List<String>): Boolean = args.size == 2

fun isCorrectDeleteCommand(args: List<String>): Boolean = (args.size == 2 && args[1].toIntOrNull() != null)

fun isCorrectMailCommand(args: List<String>): Boolean =
    (args[1] == "-to" && args[3] == "-header" && args[5] == "-content")

