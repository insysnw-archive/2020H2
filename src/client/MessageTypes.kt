package client

import common.Message
import common.TextMessage
import kotlin.reflect.KClass
import common.MessageTypes as MessageTypesInterface

class CurrencyData(type: MessageTypes, var code: String, var rate: String) : Message(type)

enum class MessageTypes(
        override val code: Int,
        override val kClass: KClass<out Message> = Message::class
) : MessageTypesInterface {
    IncorrectRateValue(-3),
    CurrencyAlreadyExist(-2),
    CurrencyNotExist(-1),

    ConnectionRequest(0),
    ConnectionResponse(1),
    DisconnectionRequest(2),
    CurrenciesListRequest(3),
    CurrenciesListResponse(4, TextMessage::class),
    AddCurrencyRequest(5, TextMessage::class),
    AddCurrencyResponse(6),
    RemoveCurrencyRequest(7, TextMessage::class),
    RemoveCurrencyResponse(8),
    AddRateRequest(9, CurrencyData::class),
    AddRateResponse(10),
    HistoryRequest(11, TextMessage::class),
    HistoryResponse(12, TextMessage::class)
}