#pragma once

#include "message.h"

#include <functional>
#include <map>
#include <mutex>
#include <string>

class MessageBuilder {
 public:
    using ReceivedMessageCallback = std::function<void(int, Message)>;

 public:
    MessageBuilder() = default;

    void setOnFullReadCallback(ReceivedMessageCallback callback) noexcept;

    void append(int from, char * data, size_t size) noexcept;

    void removeSender(int sender) noexcept;

 private:
    std::mutex mMutex;
    std::map<int, std::string> mIncompleteMessages;
    ReceivedMessageCallback mFullCallback;
};
