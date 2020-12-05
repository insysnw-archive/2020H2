#pragma once

#include "looped_thread.h"
#include "setup.h"

class IConnectionHandler {
public:
    virtual ~IConnectionHandler() noexcept = default;

    virtual void onNewConnection(int socket) noexcept = 0;
};

class ConnectionListener : public LoopedThread {
public:
    explicit ConnectionListener(
        IConnectionHandler & handler,
        const ConnectionSetup & setup) noexcept;

    ~ConnectionListener() noexcept;

private:
    void onStop() noexcept override;

    void onThreadStart() noexcept override;

    void threadStep() noexcept override;

    void onThreadFinish() noexcept override;

private:
    IConnectionHandler & mHandler;
    ConnectionSetup mSetup;
    int mSocket;
};
