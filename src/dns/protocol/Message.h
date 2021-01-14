//
// Created by Roman Svechnikov on 14.01.2021.
//

#ifndef NETLAB2_MESSAGE_H
#define NETLAB2_MESSAGE_H

#include <cstring>
#include <vector>
#include "Dns.h"

class Message {

public:

    Message(const char *buffer, long size);

    [[nodiscard]] uint16_t getId() const;

    [[nodiscard]] MessageType getMessageType() const;

    [[nodiscard]] Opcode getOpcode() const;

    [[nodiscard]] bool isAuthoritativeAnswer() const;

    [[nodiscard]] bool wasTruncated() const;

    [[nodiscard]] bool isRecursionDesired() const;

    [[nodiscard]] bool isRecursionAvailable() const;

    [[nodiscard]] Rcode getResponseCode() const;

    [[nodiscard]] uint16_t getQuestionEntriesCount() const;

    [[nodiscard]] uint16_t getResourceRecordsCount() const;

    [[nodiscard]] uint16_t getNameServerRRCount() const;

    [[nodiscard]] uint16_t getAdditionalRRCount() const;

    [[nodiscard]] const std::vector<Query> &getQueries() const;

private:
    DnsHeader header;
    std::vector<Query> queries{};

    int decodeQuery(const char *buffer);
};


#endif //NETLAB2_MESSAGE_H
