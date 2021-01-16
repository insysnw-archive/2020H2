package server

import common.Message
import common.TextMessage
import kotlin.reflect.KClass
import common.MessageTypes as MessageTypesInterface

enum class MessageTypes(
        override val code: Int,
        override val kClass: KClass<out Message> = Message::class
) : MessageTypesInterface {
    UserAlreadyConnected(-2),
    BadTestNumber(-1),

    ConnectionRequest(0, TextMessage::class),
    ConnectionResponse(1),
    DisconnectionRequest(2),
    LastResultsRequest(3),
    LastResultsResponse(4, TextMessage::class),
    TestsListRequest(5),
    TestsListResponse(6, TextMessage::class),
    TestRequest(7, TextMessage::class),
    Question(8, TextMessage::class),
    Answer(9, TextMessage::class)
}
