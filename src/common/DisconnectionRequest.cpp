//
// Created by Roman Svechnikov on 12.09.2020.
//

#include "DisconnectionRequest.h"

std::pair<char *, size_t> DisconnectionRequest::serialize() const {
    packetlen_t dataLength = sizeof(headers::header_t) + sizeof(packetlen_t);
    auto *data = static_cast<char *>(malloc(dataLength));
    data[0] = headers::DISCONNECTION_REQUEST;
    memcpy(data + sizeof(headers::header_t), &dataLength, sizeof(packetlen_t));
    return std::pair<char *, size_t>(data, dataLength);
}