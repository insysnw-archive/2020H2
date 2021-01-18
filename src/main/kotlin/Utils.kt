import java.text.SimpleDateFormat
import java.util.Date

const val ERROR_STRING = "Error: "
const val SUCCESS_STRING = "Success: "

const val CONSOLE_LINE = "egmail> "

fun printConsoleLine() = print(CONSOLE_LINE)

fun printHelp() {
    printConsoleLine()
    println(
        "commands:\n    help - show help.\n" +
            "    login <email> - command to authorize in server.\n" +
            "    send -to <reciever> -header <mail header> -content <mail content> - command to send mail.\n" +
            "    read - get all mails.\n" +
            "    delete <id> - delete mail by id. (id in square brackets)\n" +
            "    quit - quit from server and unauthorize.\n" +
            "    exit - to quit and exit program"
    )
}

fun printIncorrectFormatError(command: String?) {
    printConsoleLine()
    println("Incorrect command: $command")
}

fun printNotAuthError() {
    printConsoleLine()
    println("You are not authorized. Use command <login> to authorize.")
}

fun getCurrTimeStr(): String = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date())