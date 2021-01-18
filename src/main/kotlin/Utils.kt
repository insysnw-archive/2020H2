import java.text.SimpleDateFormat
import java.util.Date

const val ERROR_STRING = "Error: "
const val SUCCESS_STRING = "Success: "

const val CONSOLE_LINE = "auction> "

fun printConsoleLine() = print(CONSOLE_LINE)

fun printHelp() {
    printConsoleLine()
    println(
                "    login -name <user_name> -type <user_type> - command to authorize in server as participant or steward.\n" +
                "    bet -name <item_name> -price <item_price> -username <participant_name> - command to add item.\n" +
                "    read - get list items.\n" +
                "    add -name <item_name> -price <item_price> - add new item\n" +
                "    stop - stop auction and show results.\n" +
                "    exit - to quit and exit program" +
                        "    help - get info\n"
    )
}

fun printIncorrectFormatError(command: String?) {
    printConsoleLine()
    println("Incorrect command: $command")
}

fun printNotAuthError() {
    printConsoleLine()
    println("You don't have permission")
}

fun getCurrTimeStr(): String = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date())