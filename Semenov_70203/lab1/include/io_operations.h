#pragma once

#include "message.h"
#include "message_builder.h"
#include "task.h"

#include <functional>
#include <memory>
#include <vector>

class IoReadTask : public ITask {
 public:
    static constexpr auto BUFFER_SIZE = 64;
    using TaskReadCallback = std::function<void(int)>;

 public:
    explicit IoReadTask(
        int socket,
        MessageBuilder * builder,
        TaskReadCallback callback) noexcept;

    void run() noexcept override;

 private:
    int mSocket;
    MessageBuilder * mBuilder;
    TaskReadCallback mCallback;
};

class IoWriteTask : public ITask {
 public:
    using SharedMessage = std::shared_ptr<Message>;
    using SharedSockets = std::shared_ptr<int[]>;
    using WriteTaskList = std::vector<std::unique_ptr<IoWriteTask>>;

 public:
    explicit IoWriteTask(int socket, const Message & message) noexcept;

    explicit IoWriteTask(
        const std::vector<int> & sockets,
        const Message & message) noexcept;

    IoWriteTask(const IoWriteTask &) noexcept = default;

    void run() noexcept override;

    WriteTaskList split(int splits) noexcept;

 private:
    SharedSockets mSockets;
    SharedMessage mMessage;
    mutable int mFrom;
    mutable int mTo;
};
