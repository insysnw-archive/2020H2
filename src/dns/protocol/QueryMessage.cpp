//
// Created by Roman Svechnikov on 15.01.2021.
//

#include "QueryMessage.h"

QueryMessage::QueryMessage(const char *buffer, long size) : Message() {
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

int QueryMessage::decodeQuery(const char *buffer) {
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
    query.type = static_cast<QType>((uint16_t) bswap_16((uint16_t) query.type));
    std::memcpy(&query.qClass, buffer + sizeof(query.type), sizeof(query.qClass));
    query.qClass = static_cast<QClass>((uint16_t) bswap_16((uint16_t) query.qClass));
    queries.push_back(query);
    return totalLength + (int) sizeof(query.type) + (int) sizeof(query.qClass);
}

const std::vector<Query> &QueryMessage::getQueries() const {
    return queries;
}
