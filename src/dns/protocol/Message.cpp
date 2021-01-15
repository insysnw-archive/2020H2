//
// Created by Roman Svechnikov on 14.01.2021.
//

#include "Message.h"

MessageType Message::getMessageType() const {
    return static_cast<MessageType>((header.flags & 0b1000000000000000u) >> 15u);
}

void Message::setMessageType(MessageType type) {
    if (type == MessageType::RESPONSE) {
        header.flags |= 0b1000000000000000u;
    } else {
        header.flags &= ~0b1000000000000000u;
    }
}

Opcode Message::getOpcode() const {
    return static_cast<Opcode>((header.flags & 0b0111100000000000u) >> 11u);
}

void Message::setOpcode(Opcode opcode) {
    header.flags |= (uint16_t) (static_cast<std::uint16_t>(opcode) << 11u);
}

bool Message::isAuthoritativeAnswer() const {
    return header.flags & 0b0000010000000000u;
}

void Message::setAuthoritativeAnswer(bool value) {
    if (value) {
        header.flags |= 0b0000010000000000u;
    } else {
        header.flags &= ~0b0000010000000000u;
    }
}

bool Message::wasTruncated() const {
    return header.flags & 0b0000001000000000u;
}

void Message::setTruncated(bool value) {
    if (value) {
        header.flags |= 0b0000001000000000u;
    } else {
        header.flags &= ~0b0000001000000000u;
    }
}

bool Message::isRecursionDesired() const {
    return header.flags & 0b0000000100000000u;
}

void Message::setRecursionDesired(bool value) {
    if (value) {
        header.flags |= 0b0000000100000000u;
    } else {
        header.flags &= ~0b0000000100000000u;
    }
}

bool Message::isRecursionAvailable() const {
    return header.flags & 0b0000000010000000u;
}

void Message::setRecursionAvailable(bool value) {
    if (value) {
        header.flags |= 0b0000000010000000u;
    } else {
        header.flags &= ~0b0000000010000000u;
    }
}

Rcode Message::getResponseCode() const {
    return static_cast<Rcode>(header.flags & 0b0000000000001111u);
}

void Message::setResponseCode(Rcode code) {
    header.flags |= static_cast<std::uint16_t>(code);
}

uint16_t Message::getQuestionEntriesCount() const {
    return header.qdCount;
}

void Message::setQuestionEntriesCount(uint16_t count) {
    header.qdCount = count;
}

uint16_t Message::getResourceRecordsCount() const {
    return header.anCount;
}

void Message::setResourceRecordsCount(uint16_t count) {
    header.anCount = count;
}

uint16_t Message::getNameServerRRCount() const {
    return header.nsCount;
}

void Message::setNameServerRRCount(uint16_t count) {
    header.nsCount = count;
}

uint16_t Message::getAdditionalRRCount() const {
    return header.arCount;
}

void Message::setAdditionalRRCount(uint16_t count) {
    header.arCount = count;
}

uint16_t Message::getId() const {
    return header.id;
}

void Message::setId(uint16_t id) {
    header.id = id;
}
