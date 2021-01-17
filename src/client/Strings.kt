package client

object Strings {
    const val SOCKET_NOT_CREATED = "Не удалось создать сокет"
    val CONNECTION_SUCCESSFUL = """
        Соединение установлено. Команды:

        * Добавить: + USD
        * Удалить: - USD
        * Список: *
        * Котировки: ~ USD 1.42
        * История валюты: ? USD
        * Выйти: !
    """.trimIndent()
    const val UNKNOWN_COMMAND = "Команда не распознана"
    const val ADDED_SUCCESSFUL = "Валюта успешно добавлена"
    const val REMOVED_SUCCESSFUL = "Валюта успешно удалена"
    const val RATE_ADDED_SUCCESSFUL = "Курс валюты успешно задан"
    const val CURRENCY_ALREADY_EXIST = "Валюта с таким кодом уже задана"
    const val CURRENCY_NOT_EXIST = "Валюты с таким кодом не существует"
    const val STATUS_OK = "Ok"
    const val STATUS_EXCEPTION = "Соединение разорвано"
    const val INCORRECT_RATE_VALUE = "Курс валюты должен быть неотрицательным числом"
    val TOO_FEW_PARAMS = { count: Int -> "Данной команде нужно $count параметров" }
}