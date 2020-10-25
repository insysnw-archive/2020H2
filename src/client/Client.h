//
// Created by Roman Svechnikov on 10.09.2020.
//

#ifndef NETLABS_LAB1_CLIENT_H
#define NETLABS_LAB1_CLIENT_H

#ifdef _WIN32

#include <winsock2.h>

#else

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#endif

#include <iostream>
#include <cstdio>
#include <cstring>
#include <string>
#include <chrono>
#include <thread>
#include <atomic>
#include <functional>
#include "../common/Payload.h"
#include "../common/PacketHeaders.h"

class Client {

#ifdef _WIN32
    using socket_t = SOCKET;
#else
    using socket_t = int;
#endif

public:
    explicit Client(const std::string &username);

    ~Client();

    void connectTo(const std::string &address, uint16_t port);

    template<typename Packet, typename ...Args>
    void sendMessage(Args &&...args) const {
        auto[data, len] = Packet{std::forward<Args>(args)...}.serialize();
        send(connectionSocket, data, len, 0);
        free(data);
    }

    template<typename Packet>
    void sendMessage() const {
        auto[data, len] = Packet{}.serialize();
        send(connectionSocket, data, len, 0);
        free(data);
    }

    void stop();

    std::function<void(const ChatMessage &)> onMessageReceived = [](const ChatMessage &msg) {};

private:

    socket_t connectionSocket = -1;

    static constexpr uint16_t bufferSize = 1024;
    char buffer[bufferSize]{};

    std::atomic_bool shouldExit = false;

    const std::string &username;

    void handshake();

    void loop();

};


#endif //NETLABS_LAB1_CLIENT_H
