#include "message_builder.h"

#include "utils.h"

#include <string>

void MessageBuilder::setOnFullReadCallback(
    ReceivedMessageCallback callback) noexcept {
    std::lock_guard lock{mMutex};
    mFullCallback = callback;
}

void MessageBuilder::append(int from, char * data, size_t size) noexcept {
    std::string raw;
    std::lock_guard lock{mMutex};
    mIncompleteMessages.try_emplace(from, std::string{});
    mIncompleteMessages[from].append(data, size);
    raw = mIncompleteMessages[from];
    auto message = Message::deserialize(raw.data(), raw.size());

    if (message.has_value()) {
        auto messageSize = message->size();

        if (messageSize == raw.size())
            mIncompleteMessages.erase(from);
        else
            mIncompleteMessages[from].erase(0, messageSize);

        if (mFullCallback)
            mFullCallback(from, std::move(*message));
    }
}

void MessageBuilder::removeSender(int sender) noexcept {
    std::lock_guard lock{mMutex};
    mIncompleteMessages.erase(sender);
}
