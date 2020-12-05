#pragma once

#include "incoming_events_listener.h"

#include <QObject>

class EventListenerAdapter : public QObject, public IncomingEventHandler {
    Q_OBJECT
public:
    explicit EventListenerAdapter(int eventBufferSize, int timeout) noexcept;

    void add(int socket) noexcept;

    void oneshot(int socket) noexcept;

private:
    void onIncomingMessageFrom(int socket) noexcept override;

    void onConnectionLost(int socket) noexcept override;

signals:
    void incomingMessage();

    void connectionLost();

private:
    IncomingEventsListener mListener;
};
