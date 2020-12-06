#include "io_operations.h"

#include <unistd.h>

#include <algorithm>
#include <memory>

IoReadTask::IoReadTask(
    int socket,
    MessageBuilder * builder,
    TaskReadCallback callback) noexcept
    : mSocket{socket}, mBuilder{builder}, mCallback{callback} {}

void IoReadTask::run() noexcept {
    std::array<char, BUFFER_SIZE> buffer;
    while (true) {
        auto bytes = read(mSocket, buffer.begin(), buffer.size());
        if (bytes <= 0)
            break;
        mBuilder->append(mSocket, buffer.data(), bytes);
    }

    if (mCallback)
        mCallback(mSocket);
}

IoWriteTask::IoWriteTask(int socket, const Message & message) noexcept {
    mFrom = 0;
    mTo = 1;
    mMessage = std::make_shared<Message>(message);
    mSockets = std::make_shared<int[]>(mTo);
    mSockets[0] = socket;
}

IoWriteTask::IoWriteTask(
    const std::vector<int> & sockets,
    const Message & message) noexcept
    : mMessage{std::make_shared<Message>(message)} {
    mFrom = 0;
    mTo = sockets.size();
    mMessage = std::make_shared<Message>(message);
    mSockets = std::make_shared<int[]>(mTo);
    for (int i = 0; i < sockets.size(); i++)
        mSockets[i] = sockets[i];
}

void IoWriteTask::run() noexcept {
    auto serialized = mMessage->serialize();
    auto data = serialized.data();
    auto size = serialized.size();

    for (int i = mFrom; i < mTo; ++i) {
        size_t bytes = 0;
        do {
            auto written = write(mSockets[i], data, size);
            if (written < 0)
                break;

            bytes += written;
        } while (bytes != mMessage->size());
    }
}

IoWriteTask::WriteTaskList IoWriteTask::split(int splits) noexcept {
    const auto size = mTo - mFrom;
    const auto splitsNumber = std::min(splits, size);
    const auto part = size / splitsNumber;

    WriteTaskList tasks;
    for (auto i = 1; i < splitsNumber; ++i) {
        IoWriteTask copy{*this};
        copy.mFrom = this->mFrom + (i - 1) * part;
        copy.mTo = i * part;
        tasks.emplace_back(std::make_unique<IoWriteTask>(std::move(copy)));
    }

    mFrom = (splitsNumber - 1) * part;
    tasks.emplace_back(std::make_unique<IoWriteTask>(std::move(*this)));
    return tasks;
}
