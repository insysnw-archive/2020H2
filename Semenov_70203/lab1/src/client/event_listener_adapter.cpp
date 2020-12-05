#include "client/event_listener_adapter.h"

EventListenerAdapter::EventListenerAdapter(
    int eventBufferSize,
    int timeout) noexcept
    : mListener{*this, eventBufferSize, timeout} {
    mListener.start();
}

void EventListenerAdapter::add(int socket) noexcept {
    mListener.add(socket);
}

void EventListenerAdapter::oneshot(int socket) noexcept {
    mListener.oneshot(socket);
}

void EventListenerAdapter::onIncomingMessageFrom(int) noexcept {
    emit incomingMessage();
}

void EventListenerAdapter::onConnectionLost(int) noexcept {
    emit connectionLost();
}
