#pragma once

#include "message.h"
#include "task.h"

#include <functional>
#include <memory>
#include <vector>

class IoReadTask : public ITask {
public:
    static constexpr auto BUFFER_SIZE = 64;

public:
    using CallbackType = std::function<void(Message)>;

public:
    explicit IoReadTask(
        int socket,
        CallbackType callback = CallbackType{}) noexcept;

    void run() noexcept override;

private:
    int mSocket;
    CallbackType mCallback;
};

class IoWriteTask : public ITask {
public:
    explicit IoWriteTask(int socket, const Message & message) noexcept;

    void run() noexcept override;

private:
    int mSocket;
    Message mMessage;
};

class IoBroadcastTask : public ITask {
public:
    using SocketList = std::vector<int>;
    using SharedMessage = std::shared_ptr<Message>;
    using SharedSockets = std::shared_ptr<SocketList>;
    using BroadcastTaskList = std::vector<std::unique_ptr<IoBroadcastTask>>;

public:
    explicit IoBroadcastTask(
        const SocketList & sockets,
        const Message & message) noexcept;

    IoBroadcastTask(const IoBroadcastTask &) noexcept = default;

    void run() noexcept override;

    BroadcastTaskList split(int splits) noexcept;

private:
    SharedSockets mSockets;
    SharedMessage mMessage;
    int mFrom = 0;
    int mTo = mSockets->size();
};
