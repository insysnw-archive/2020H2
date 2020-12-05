#include "server.h"

#include "io_operations.h"
#include "message.h"
#include "setup.h"
#include "utils.h"

#include <unistd.h>

Server::Server(const EndpointSetup & setup) noexcept
    : mListener{*this, setup.connection},
      mIncomingEventsListener{*this, setup.eventBufferSize, setup.timeout},
      mWorkers{setup.parallelWorkers} {}

Server::~Server() noexcept {
    MANUAL_FINISH

    for (auto socket : mSockets)
        close(socket);

    logInfo("Server stopped");
}

void Server::join() noexcept {
    mListener.join();
    stop();
}

void Server::onStart() noexcept {
    mListener.start();
    mIncomingEventsListener.start();
    mWorkers.start();
}

void Server::onStop() noexcept {
    mWorkers.stop();
    mListener.stop();
    mIncomingEventsListener.stop();
}

void Server::onNewConnection(int socket) noexcept {
    std::lock_guard lock{mMutex};
    //makeNonBlocking(socket);
    mSockets.push_back(socket);

    mIncomingEventsListener.add(socket);
    logInfo("New connection");
}

void Server::onIncomingMessageFrom(int socket) noexcept {
    auto readTask = std::make_unique<IoReadTask>(
        socket, [this, socket](const Message & message) {
            onMessageReceived(socket, message);
        });
    mWorkers.addTask(std::move(readTask));
}

void Server::onConnectionLost(int socket) noexcept {
    std::lock_guard lock{mMutex};
    close(socket);
    auto eraseFrom =
        std::remove(std::begin(mSockets), std::end(mSockets), socket);
    mSockets.erase(eraseFrom, std::end(mSockets));

    logInfo("Connection lost");
}

void Server::onMessageReceived(int socket, const Message & message) noexcept {
    mIncomingEventsListener.oneshot(socket);

    std::lock_guard lock{mMutex};
    auto broadcastTask = std::make_unique<IoBroadcastTask>(mSockets, message);

    for (auto & subtask : broadcastTask->split(mWorkers.size()))
        mWorkers.addTask(std::move(subtask));
}
