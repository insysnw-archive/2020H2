#include "utils.h"

#include "setup.h"

#include <arpa/inet.h>
#include <fcntl.h>
#include <getopt.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>

#include <cstring>

#ifndef CHAT_LOG_ERROR
#define CHAT_LOG_ERROR 0
#endif

#ifndef CHAT_LOG_INFO
#define CHAT_LOG_INFO 0
#endif

#if CHAT_LOG_INFO | CHAT_LOG_ERROR
#include <cerrno>
#include <iostream>
#include <mutex>
#endif

void logError([[maybe_unused]] std::string_view source) noexcept {
#if CHAT_LOG_ERROR
    static std::mutex lMutex;
    std::lock_guard lock{lMutex};

    std::cerr << "ERROR: <" << source << "> " << strerror(errno) << std::endl;
#endif
}

void logInfo([[maybe_unused]] std::string_view info) noexcept {
#if CHAT_LOG_INFO
    static std::mutex lMutex;
    std::lock_guard lock{lMutex};

    std::cout << "INFO: " << info << std::endl;
#endif
}

void makeNonBlocking(int fd) noexcept {
    auto flags = fcntl(fd, F_GETFL);
    if (fcntl(fd, F_SETFL, flags | O_NONBLOCK) < 0)
        logError("fcntl");
}

std::optional<sockaddr_in> getSocketAddress(const ConnectionSetup & setup) {
    sockaddr_in address;
    memset(&address, 0, sizeof(address));

    address.sin_family = AF_INET;
    address.sin_port = htons(setup.port);

    if (inet_pton(AF_INET, setup.address.c_str(), &address.sin_addr) <= 0)
        return std::nullopt;

    return address;
}

ConnectionResponse bindedSocket(const ConnectionSetup & setup) noexcept {
    auto socket = ::socket(AF_INET, setup.type, 0);
    auto address = getSocketAddress(setup);
    if (!address.has_value()) {
        close(socket);
        return ConnectionResponse{NET_ERROR::CRITICAL};
    }

    auto casted = reinterpret_cast<sockaddr *>(&address);
    int option = 1;
    if (setsockopt(socket, SOL_SOCKET, SO_REUSEADDR, &option, sizeof(option)) <
        0)
        logError("setsockopt");

    if (bind(socket, casted, sizeof(address)) < 0) {
        logError("bind");
        close(socket);
        return ConnectionResponse{NET_ERROR::CRITICAL};
    }

    logInfo(
        "Binding address: " + setup.address + ":" + std::to_string(setup.port));

    return ConnectionResponse{socket};
}

ConnectionResponse listeningSocket(const ConnectionSetup & setup) noexcept {
    auto response = bindedSocket(setup);
    if (response.error != NET_ERROR::NONE)
        return response;

    if (listen(response.socket, 32) < 0) {
        logError("listen");
        close(response.socket);
        return ConnectionResponse{NET_ERROR::CRITICAL};
    }

    return response;
}

ConnectionResponse connectedSocket(const ConnectionSetup & setup) noexcept {
    auto socket = ::socket(AF_INET, setup.type, 0);
    auto address = getSocketAddress(setup);
    if (!address.has_value()) {
        close(socket);
        return ConnectionResponse{NET_ERROR::CRITICAL};
    }

    auto casted = reinterpret_cast<sockaddr *>(&address);
    if (connect(socket, casted, sizeof(address)) < 0) {
        logError("connect");
        return ConnectionResponse{NET_ERROR::TEMPORARY};
    }

    logInfo(
        "Connected to address: " + setup.address + ":" +
        std::to_string(setup.port));

    return ConnectionResponse{socket};
}

std::optional<EndpointSetup>
getSetup(int argc, char * argv[], std::string_view ip, int port) noexcept {
    EndpointSetup setup;
    setup.connection.address = ip;
    setup.connection.port = port;
    setup.connection.type = SOCK_STREAM;
    setup.timeout = 1000;
    setup.eventBufferSize = 32;
    setup.parallelWorkers = 4;

    auto envIp = getenv("CHAT_IP");
    auto envPort = getenv("CHAT_PORT");
    if (envIp != nullptr)
        setup.connection.address = envIp;
    if (envPort != nullptr)
        setup.connection.port = std::stoi(envPort);

    const auto shortOptions = "hi:p:w:";
    const option longOptions[] = {
        {"help", no_argument, nullptr, 'h'},
        {"ip", required_argument, nullptr, 'i'},
        {"port", required_argument, nullptr, 'p'},
        {"workers", required_argument, nullptr, 'w'},
        {nullptr, no_argument, nullptr, 0}};

    while (true) {
        const auto option =
            getopt_long(argc, argv, shortOptions, longOptions, nullptr);

        if (option < 0)
            break;

        switch (option) {
            case 'i': setup.connection.address = optarg; break;
            case 'p': setup.connection.port = std::stoi(optarg); break;
            case 'w': setup.parallelWorkers = std::stoi(optarg); break;
            case 'h': [[fallthrough]];
            default:
                std::cout
                    << "-h --help    : print this message\n"
                    << "-i --ip      : set an ip address (default 127.0.0.1)\n"
                    << "-p --port    : set a port (default 50000)\n"
                    << "-w --workers : set a number of workers (server only, "
                       "default 4)"
                    << std::endl;
                return std::nullopt;
        }
    }

    return setup;
}
