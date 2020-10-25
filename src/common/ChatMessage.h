//
// Created by Roman Svechnikov on 12.09.2020.
//

#ifndef NETLABS_CHATMESSAGE_H
#define NETLABS_CHATMESSAGE_H


#include <string>
#include <cstring>
#include "PacketHeaders.h"

class ChatMessage {

public:
    explicit ChatMessage(char *data);

    ChatMessage(std::string name, std::string message);

    [[nodiscard]] const std::string &getName() const;

    [[nodiscard]] const std::string &getMessage() const;

    [[nodiscard]] std::pair<char *, size_t> serialize() const;

private:
    std::string username;
    std::string message;
};


#endif //NETLABS_CHATMESSAGE_H
