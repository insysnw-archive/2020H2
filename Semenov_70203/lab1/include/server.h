#pragma once

#include "connection_listener.h"
#include "incoming_events_listener.h"
#include "manual_control.h"
#include "message_builder.h"
#include "workers_pool.h"

#include <mutex>
#include <vector>

struct EndpointSetup;
struct Message;

class Server : public IConnectionHandler,
               public IncomingEventHandler,
               public ManualControl {
 public:
    using SocketList = std::vector<int>;

 public:
    explicit Server(const EndpointSetup & setup) noexcept;

    ~Server() noexcept;

    void join() noexcept override;

 private:
    void onStart() noexcept override;

    void onStop() noexcept override;

    void onNewConnection(int socket) noexcept override;

    void onIncomingMessageFrom(int socket) noexcept override;

    void onConnectionLost(int socket) noexcept override;

    void onMessageReceived(int socket, Message message) noexcept;

 private:
    std::mutex mMutex;
    SocketList mSockets;
    MessageBuilder mMessageBuilder;

    ConnectionListener mListener;
    IncomingEventsListener mIncomingEventsListener;
    WorkersPool mWorkers;
};
