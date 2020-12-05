#pragma once

#include "looped_thread.h"

#include <sys/epoll.h>

#include <memory>
#include <vector>

class IncomingEventHandler {
public:
    virtual ~IncomingEventHandler() noexcept = default;

    virtual void onIncomingMessageFrom(int socket) noexcept = 0;

    virtual void onConnectionLost(int socket) noexcept = 0;
};

class IncomingEventsListener : public LoopedThread {
public:
    IncomingEventsListener(
        IncomingEventHandler & handler,
        int eventBufferSize,
        int timeout);

    ~IncomingEventsListener() noexcept;

    void add(int socket) noexcept;

    void oneshot(int socket) noexcept;

private:
    void onThreadStart() noexcept override;

    void threadStep() noexcept override;

    void onThreadFinish() noexcept override;

private:
    IncomingEventHandler & mHandler;

    std::unique_ptr<epoll_event[]> mEventBuffer;
    const int mTimeout;
    const int mBufferSize;

    int mEpoll;
};
