#include "io_operations.h"

#include "utils.h"

#include <unistd.h>

#include <algorithm>
#include <memory>

IoReadTask::IoReadTask(int socket, CallbackType callback) noexcept
    : mSocket{socket}, mCallback{callback} {}

void IoReadTask::run() noexcept {
    std::array<char, BUFFER_SIZE> buffer;
    std::vector<char> rawMessage;
    int bytes;

    do {
        bytes = read(mSocket, buffer.begin(), buffer.size());

        if (bytes < 0) {
            logError("read");
            return;
        }

        std::copy(
            buffer.begin(), buffer.begin() + bytes,
            std::back_inserter(rawMessage));
    } while (buffer[bytes - 1] != '\0');

    logInfo("Received " + std::to_string(rawMessage.size()) + " bytes");

    if (mCallback) {
        auto readMessage =
            Message::deserialize(rawMessage.data(), rawMessage.size());
        readMessage.datetime = time(nullptr);
        mCallback(readMessage);
    }
}

IoWriteTask::IoWriteTask(int socket, const Message & message) noexcept
    : mSocket{socket}, mMessage{message} {}

void IoWriteTask::run() noexcept {
    auto serialized = mMessage.serialize();
    if (write(mSocket, serialized.data(), serialized.size()) < 0)
        logError("write");
}

IoBroadcastTask::IoBroadcastTask(
    const SocketList & sockets,
    const Message & message) noexcept
    : mSockets{std::make_shared<SocketList>(sockets)},
      mMessage{std::make_shared<Message>(message)} {}

void IoBroadcastTask::run() noexcept {
    auto serialized = mMessage->serialize();

    for (int i = mFrom; i < mTo; ++i) {
        if (write((*mSockets)[i], serialized.data(), serialized.size()) < 0)
            logError("write");
    }
}

IoBroadcastTask::BroadcastTaskList IoBroadcastTask::split(int splits) noexcept {
    const auto size = mTo - mFrom;
    const auto splitsNumber = std::min(splits, size);
    const auto part = size / splitsNumber;

    BroadcastTaskList tasks;
    for (auto i = 1; i < splitsNumber; ++i) {
        IoBroadcastTask copy{*this};
        copy.mFrom = this->mFrom + (i - 1) * part;
        copy.mTo = i * part;
        tasks.emplace_back(std::make_unique<IoBroadcastTask>(std::move(copy)));
    }

    mFrom = (splitsNumber - 1) * part;
    tasks.emplace_back(std::make_unique<IoBroadcastTask>(std::move(*this)));
    return tasks;
}
