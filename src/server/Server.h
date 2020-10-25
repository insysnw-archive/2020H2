//
// Created by Roman Svechnikov on 10.09.2020.
//

#ifndef NETLABS_LAB1_SERVER_H
#define NETLABS_LAB1_SERVER_H

#ifdef _WIN32

#include <winsock2.h>

#else

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include <unistd.h>

#endif

#include <cstdint>
#include <thread>
#include <string>
#include <iostream>
#include <mutex>
#include <atomic>
#include <chrono>
#include <unordered_set>
#include <bits/unordered_map.h>
#include <queue>
#include "../common/Payload.h"

class Server {

#ifdef _WIN32
    using socket_t = SOCKET;
    using socklen_t = int;
#else
    using socket_t = int;
#endif

public:

    explicit Server(uint16_t port);

    ~Server();

    void start();

    void stop();

private:

    socket_t listenSocket = -1;

    static constexpr uint16_t bufferSize = 1024;

    uint16_t port;

    std::atomic_bool shouldExit = false;

    std::unordered_set<std::string> users = {};
    std::unordered_map<socket_t, std::thread> handlerThreads = {};
    std::queue<socket_t> clientsToFree = {};

    void loop();

    void handleClient(socket_t clientSocket);

    void userInput();

    template<typename Packet, typename ...Args>
    void sendMessage(socket_t socket, Args &&...args) {
        auto[data, len] = Packet{std::forward<Args>(args)...}.serialize();
        send(socket, data, len, 0);
        free(data);
    }

};


#endif //NETLABS_LAB1_SERVER_H
