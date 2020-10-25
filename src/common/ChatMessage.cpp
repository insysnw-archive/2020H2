//
// Created by Roman Svechnikov on 12.09.2020.
//

#include "ChatMessage.h"

ChatMessage::ChatMessage(char *data) {
    int nameLength;
    memcpy(&nameLength, data + sizeof(headers::header_t) + sizeof(packetlen_t), sizeof(nameLength));
    char nameArray[nameLength + 1];
    memcpy(nameArray, data + sizeof(headers::header_t) + sizeof(packetlen_t) + sizeof(nameLength), (size_t) nameLength);
    nameArray[nameLength] = '\0';
    username = std::string{nameArray};

    int msgLength;
    memcpy(&msgLength, data + sizeof(headers::header_t) + sizeof(packetlen_t)
                       + sizeof(nameLength) + nameLength, sizeof(int));
    char msgArray[msgLength + 1];
    memcpy(msgArray, data + sizeof(headers::header_t) + sizeof(packetlen_t)
                     + sizeof(nameLength) + nameLength + sizeof(msgLength), (size_t) msgLength);
    msgArray[msgLength] = '\0';
    message = std::string{msgArray};
}

ChatMessage::ChatMessage(std::string name, std::string message)
        : username(std::move(name)), message(std::move(message)) {

}

const std::string &ChatMessage::getName() const {
    return username;
}

const std::string &ChatMessage::getMessage() const {
    return message;
}

std::pair<char *, size_t> ChatMessage::serialize() const {
    int nameLength = username.size();
    int msgLength = message.size();
    packetlen_t dataLength =
            sizeof(headers::header_t) + sizeof(packetlen_t)
            + sizeof(nameLength) + nameLength + sizeof(msgLength) + msgLength;
    auto *data = static_cast<char *>(malloc(dataLength));

    data[0] = headers::CHAT_MESSAGE;
    memcpy(data + sizeof(headers::header_t), &dataLength, sizeof(packetlen_t));
    memcpy(data + sizeof(headers::header_t) + sizeof(packetlen_t), &nameLength, sizeof(int));
    memcpy(data + sizeof(headers::header_t) + sizeof(packetlen_t) + sizeof(nameLength), username.data(), nameLength);
    memcpy(data + sizeof(headers::header_t) + sizeof(packetlen_t)
           + sizeof(nameLength) + nameLength, &msgLength, sizeof(int));
    memcpy(data + sizeof(headers::header_t) + +sizeof(packetlen_t)
           + sizeof(nameLength) + nameLength + sizeof(msgLength), message.data(), msgLength);
    return std::pair<char *, size_t>(data, dataLength);
}
