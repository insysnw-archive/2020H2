//
// Created by Roman Svechnikov on 12.09.2020.
//

#include <cstdlib>
#include <cstring>
#include "ConnectionResponse.h"

ConnectionResponse::ConnectionResponse(char *data) {
    memcpy(&status, data + sizeof(headers::header_t) + sizeof(packetlen_t), sizeof(status));
}

ConnectionResponse::ConnectionResponse(ConnectionStatus status) : status(status) {

}

const ConnectionStatus &ConnectionResponse::getStatus() const {
    return status;
}

std::pair<char *, size_t> ConnectionResponse::serialize() const {
    packetlen_t dataLength = sizeof(headers::header_t) + sizeof(packetlen_t) + sizeof(status);
    auto *data = static_cast<char *>(malloc(dataLength));
    data[0] = headers::CONNECTION_RESPONSE;
    memcpy(data + sizeof(headers::header_t), &dataLength, sizeof(packetlen_t));
    memcpy(data + sizeof(headers::header_t) + sizeof(packetlen_t), &status, sizeof(status));
    return std::pair<char *, size_t>(data, dataLength);
}
