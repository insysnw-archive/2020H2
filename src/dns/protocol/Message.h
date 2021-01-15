//
// Created by Roman Svechnikov on 14.01.2021.
//

#ifndef NETLAB2_MESSAGE_H
#define NETLAB2_MESSAGE_H

#include <cstring>
#include <string>
#include <byteswap.h>
#include <vector>
#include "Dns.h"

class Message {

public:

    [[nodiscard]] uint16_t getId() const;

    void setId(uint16_t id);

    [[nodiscard]] MessageType getMessageType() const;

    void setMessageType(MessageType type);

    [[nodiscard]] Opcode getOpcode() const;

    void setOpcode(Opcode opcode);

    [[nodiscard]] bool isAuthoritativeAnswer() const;

    void setAuthoritativeAnswer(bool value);

    [[nodiscard]] bool wasTruncated() const;

    void setTruncated(bool value);

    [[nodiscard]] bool isRecursionDesired() const;

    void setRecursionDesired(bool value);

    [[nodiscard]] bool isRecursionAvailable() const;

    void setRecursionAvailable(bool value);

    [[nodiscard]] Rcode getResponseCode() const;

    void setResponseCode(Rcode code);

    [[nodiscard]] uint16_t getQuestionEntriesCount() const;

    void setQuestionEntriesCount(uint16_t count);

    [[nodiscard]] uint16_t getResourceRecordsCount() const;

    void setResourceRecordsCount(uint16_t count);

    [[nodiscard]] uint16_t getNameServerRRCount() const;

    void setNameServerRRCount(uint16_t count);

    [[nodiscard]] uint16_t getAdditionalRRCount() const;

    void setAdditionalRRCount(uint16_t count);

protected:
    DnsHeader header {};
};


#endif //NETLAB2_MESSAGE_H
