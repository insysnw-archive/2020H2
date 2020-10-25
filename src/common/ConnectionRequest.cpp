//
// Created by Roman Svechnikov on 12.09.2020.
//

#include "ConnectionRequest.h"


ConnectionRequest::ConnectionRequest(char *data) {
    unsigned int packetLength;
    memcpy(&packetLength, data + sizeof(headers::header_t), sizeof(packetlen_t));
    const unsigned int nameLength = packetLength - sizeof(headers::header_t) - sizeof(packetlen_t);
    char nameArray[nameLength + 1];
    memcpy(nameArray, data + sizeof(headers::header_t) + sizeof(packetlen_t), nameLength);
    nameArray[nameLength] = '\0';
    username = std::string{nameArray};
}

ConnectionRequest::ConnectionRequest(std::string name) : username(std::move(name)) {

}

const std::string &ConnectionRequest::getName() const {
    return username;
}

std::pair<char *, size_t> ConnectionRequest::serialize() const {
    packetlen_t dataLength = sizeof(headers::header_t) + sizeof(packetlen_t) + username.size();
    auto *data = static_cast<char *>(malloc(dataLength));
    data[0] = headers::CONNECTION_REQUEST;
    memcpy(data + sizeof(headers::header_t), &dataLength, sizeof(dataLength));
    memcpy(data + sizeof(headers::header_t) + sizeof(dataLength), username.data(), username.size());
    return std::pair<char *, size_t>(data, dataLength);
}
