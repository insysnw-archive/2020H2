#include "connection_listener.h"

#include "setup.h"
#include "utils.h"

#include <unistd.h>

ConnectionListener::ConnectionListener(
    IConnectionHandler & handler,
    const ConnectionSetup & setup) noexcept
    : mHandler{handler}, mSetup{setup} {}

ConnectionListener::~ConnectionListener() noexcept {
    MANUAL_FINISH
}

void ConnectionListener::onStop() noexcept {
    if (mSocket >= 0 && shutdown(mSocket, SHUT_RD))
        logError("shutdown");
}

void ConnectionListener::onThreadStart() noexcept {
    mSocket = listeningSocket(mSetup).socket;
    if (mSocket < 0)
        stop();
}

void ConnectionListener::threadStep() noexcept {
    auto socket = accept(mSocket, nullptr, nullptr);
    if (isStopped())
        return;

    if (socket < 0) {
        logError("accept");
    }
    mHandler.onNewConnection(socket);
}

void ConnectionListener::onThreadFinish() noexcept {
    if (mSocket >= 0 && close(mSocket) < 0)
        logError("close");
}
