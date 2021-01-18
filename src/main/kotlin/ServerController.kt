import data.itemsStorage
import data.participantStorage
import data.stewardStorage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import model.Item
import model.User
import java.net.Socket

const val SUCCESS_CODE: Byte = 0
const val ERROR_CODE: Byte = 1

fun readItems(clientSocket: Socket) {
    println("ListOfItems")
    itemsStorage.forEach {
        print(it)
    }
    clientSocket.getOutputStream()
        .write(
        byteArrayOf(SUCCESS_CODE) + Json.encodeToString(
            ListSerializer(Item.serializer()), itemsStorage
        ).toByteArray())
}

fun changePrice(clientSocket: Socket, buf: ByteArray, userName: String) {
    println("bet")
    try {
        val item = Json.decodeFromString<Item>(buf.getMsg())
        itemsStorage.forEach {
            if (it.name == item.name && it.price < item.price && participantStorage.containsKey(item.owner)) {
                itemsStorage.remove(it)
                itemsStorage.add(item)
                clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(ServerMessage("Поднял ставки для лота")))
            }
            else
                clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(ServerMessage("Не выполнены условия для поднятия ставки")))
        }
    } catch (e: Exception) {
        println(e.message)
    }


}

fun auth(buf: ByteArray, clientSocket: Socket) {
    val typeReq = buf.first()
    if (typeReq == 0.toByte()) {
        val emailStr = buf.getMsg()
        var emailData: User? = null
        try {
            emailData = Json.decodeFromString<User>(emailStr)
        } catch (e: java.lang.Exception) {
            println(e.message)
        }
        if (emailData != null) {
            println("login new client: ${emailData.name}")
            when (emailData.type) {
                "steward" -> {
                    stewardStorage[emailData.name] = clientSocket
                }
                "participant" -> {
                    participantStorage[emailData.name] = clientSocket
                }
                else -> clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(ServerMessage("Auth error")))
            }
            clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(welcomeMsg()))
            listenClient(clientSocket,emailData.name)
            }
         else {
            clientSocket.getOutputStream().write(ERROR_CODE.toServerResponse(ServerMessage("Auth error")))
            listenClient(clientSocket, null)
        }
    } else {
        auth(buf, clientSocket)
    }
}

fun addItem(buf: ByteArray, clientSocket: Socket) {
    println("addItem")
    val item = Json.decodeFromString<Item>(buf.getMsg())
    itemsStorage.add(item)
    clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(ServerMessage("Added new item")))
}

fun stopAuction(clientSocket: Socket) {
    print("Торги окончены, результаты торгов \n")
    itemsStorage.forEach {
        print(it)
    }
    clientSocket.getOutputStream()
        .write(
            byteArrayOf(SUCCESS_CODE) + Json.encodeToString(
                ListSerializer(Item.serializer()), itemsStorage
            ).toByteArray())
    itemsStorage.clear()
}

fun quit(clientSocket: Socket, userName: String) {
    print("quit")
    participantStorage.remove(userName)
    stewardStorage.remove(userName)
    clientSocket.getOutputStream().write(SUCCESS_CODE.toServerResponse(getQuitMsg(userName)))
}


