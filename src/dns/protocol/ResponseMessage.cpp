//
// Created by Roman Svechnikov on 15.01.2021.
//

#include "ResponseMessage.h"


ResponseMessage::ResponseMessage(uint16_t id) : records() {
    setId(id);
    setMessageType(MessageType::RESPONSE);
    setOpcode(Opcode::QUERY);
    setAuthoritativeAnswer(false);
    setTruncated(false);
    setRecursionDesired(false);
    setRecursionAvailable(false);
    setResponseCode(Rcode::NOT_IMPLEMENTED);
    setQuestionEntriesCount(0);
    setResourceRecordsCount(0);
    setNameServerRRCount(0);
    setAdditionalRRCount(0);
}

std::pair<char *, size_t> ResponseMessage::serialize() const {
    auto *buffer = static_cast<char *>(std::malloc(512));
    DnsHeader tempHeader {}; // to inverse the byte order
    tempHeader.id = bswap_16(header.id);
    tempHeader.flags = bswap_16(header.flags);
    tempHeader.qdCount = bswap_16(header.qdCount);
    tempHeader.anCount = bswap_16(header.anCount);
    tempHeader.nsCount = bswap_16(header.nsCount);
    tempHeader.arCount = bswap_16(header.arCount);
    std::memcpy(buffer, &tempHeader, sizeof(tempHeader));
    int totalSize = sizeof(tempHeader);

    int offset = 0;
    for (const auto &record : records) {
        offset += encodeResourceRecord(record, buffer + totalSize + offset);
    }
    return std::make_pair(buffer, totalSize + offset);
}

int ResponseMessage::encodeResourceRecord(const ResourceRecord &record, char *buffer) const {
    int start = 0;
    int end;
    uint16_t totalLength = 0;
    while ((end = record.name.find('.', start)) != std::string::npos) {
        *buffer++ = (uint8_t) (end - start); // label length
        totalLength += 1;
        for (int i = start; i < end; i++) {
            *buffer++ = record.name[i]; // label
            totalLength += 1;
        }
        start = end + 1; // skip '.'
    }
    *buffer++ = (uint8_t) (record.name.size() - start);
    totalLength += 1;
    for (int i = start; i < record.name.size(); i++) {
        *buffer++ = record.name[i]; // last label
        totalLength += 1;
    }

    uint16_t type = bswap_16((uint16_t)record.type);
    uint16_t qClass = bswap_16((uint16_t)record.qClass);
    uint32_t ttl = bswap_32(record.ttl);
    *buffer = 0;

    std::memcpy(buffer, &type, sizeof(type));
    std::memcpy(buffer + sizeof(type), &qClass, sizeof(qClass));
    std::memcpy(buffer + sizeof(type) + sizeof(qClass), &ttl, sizeof(ttl));
    uint16_t dataLength = record.recordData.size();
    uint16_t swappedLength = bswap_16(dataLength);
    std::memcpy(buffer + sizeof(type) + sizeof(qClass) + sizeof(ttl), &swappedLength, sizeof(dataLength));
    std::memcpy(buffer + sizeof(type) + sizeof(qClass) + sizeof(ttl) + sizeof(dataLength),
                record.recordData.data(), dataLength);
    return totalLength + sizeof(type) + sizeof(qClass) + sizeof(ttl) + sizeof(dataLength) + dataLength;
}

void ResponseMessage::addResourceRecord(const ResourceRecord &record) {
    records.push_back(record);
    setResourceRecordsCount(records.size());
}
