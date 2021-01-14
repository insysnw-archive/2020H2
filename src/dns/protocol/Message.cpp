//
// Created by Roman Svechnikov on 14.01.2021.
//

#include <cstdio>
#include <string>
#include <byteswap.h>
#include "Message.h"

Message::Message(const char *buffer, long size) : header() {
    if (size < sizeof(header)) {
        std::fprintf(stderr, "Error! Can't parse message\n");
    }
    std::memcpy(&header, buffer, sizeof(header));
    header.id = bswap_16(header.id);
    header.flags = bswap_16(header.flags);
    header.qdCount = bswap_16(header.qdCount);
    header.anCount = bswap_16(header.anCount);
    header.nsCount = bswap_16(header.nsCount);
    header.arCount = bswap_16(header.arCount);

    int offset = 0;
    for (int i = 0; i < header.qdCount; i++) {
        offset += decodeQuery(buffer + sizeof(header) + offset);
    }
}

int Message::decodeQuery(const char *buffer) {
    Query query{};
    int totalLength = 0;
    int length;
    do {
        length = (unsigned char) *buffer++;
        totalLength++;

        for (int i = 0; i < length; i++) {
            char c = *buffer++;
            totalLength++;
            query.name.append(1, c);
        }
        if (length != 0) {
            query.name.append(1, '.');
        }

    } while (length != 0);

    std::memcpy(&query.type, buffer, sizeof(query.type));
    query.type = static_cast<QType>((uint16_t) bswap_16(query.type));
    std::memcpy(&query.qClass, buffer + sizeof(query.type), sizeof(query.qClass));
    query.qClass = static_cast<QClass>((uint16_t) bswap_16(query.qClass));
    queries.push_back(query);
    return totalLength + (int) sizeof(query.type) + (int) sizeof(query.qClass);
}

MessageType Message::getMessageType() const {
    return static_cast<MessageType>((header.flags & 0b1000000000000000u) >> 15u);
}

Opcode Message::getOpcode() const {
    return static_cast<Opcode>((header.flags & 0b0111100000000000u) >> 11u);
}

bool Message::isAuthoritativeAnswer() const {
    return header.flags & 0b0000010000000000u;
}

bool Message::wasTruncated() const {
    return header.flags & 0b0000001000000000u;
}

bool Message::isRecursionDesired() const {
    return header.flags & 0b0000000100000000u;
}

bool Message::isRecursionAvailable() const {
    return header.flags & 0b0000000010000000u;
}

Rcode Message::getResponseCode() const {
    return static_cast<Rcode>(header.flags & 0b0000000000001111u);
}

uint16_t Message::getQuestionEntriesCount() const {
    return header.qdCount;
}

uint16_t Message::getResourceRecordsCount() const {
    return header.anCount;
}

uint16_t Message::getNameServerRRCount() const {
    return header.nsCount;
}

uint16_t Message::getAdditionalRRCount() const {
    return header.arCount;
}

uint16_t Message::getId() const {
    return header.id;
}

const std::vector<Query> &Message::getQueries() const {
    return queries;
}
