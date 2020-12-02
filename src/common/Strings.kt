package common

object Strings {
    const val SERVER_STARTED = "Сервер запущен"
    const val SERVER_NOT_STARTED = "Сервер не запущен"
    const val SOCKET_NOT_CREATED = "Не удалось создать сокет"
    const val ENTER_USERNAME = "Введите имя: "
    const val BAD_USERNAME = "Это имя содержит недопустимые символы \"[\" или \"]\". Введите другое имя: "
    const val TAKEN_USERNAME = "Это имя занято. Введите другое имя: "
    const val STATUS_OK = "Ok"
    const val STATUS_EXCEPTION = "Соединение разорвано"
    val HELLO = { username: String? -> "Привет, $username! Соединение установлено" }
}