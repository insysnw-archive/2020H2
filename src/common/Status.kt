package common

enum class Status(val code: Int, val message: String) {
    OK(0, Strings.STATUS_OK),
    EXCEPTION(-1, Strings.STATUS_EXCEPTION)
}