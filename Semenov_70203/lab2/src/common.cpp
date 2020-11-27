#include "dhcp/common.h"

#include <unistd.h>

#include <cstring>
#include <iostream>
#include <mutex>

#include "dhcp/config.h"

namespace dhcp {

void logError(std::string_view source) noexcept {
    static std::mutex lMutex;
    std::lock_guard lock{lMutex};
    std::cerr << "ERROR: <" << source << "> " << strerror(errno) << std::endl;
}

void logInfo(std::string_view info) noexcept {
    static std::mutex lMutex;
    std::lock_guard lock{lMutex};
    std::cout << "\033[33m";
    std::cout << "INFO: " << info;
    std::cout << "\033[0m" << std::endl;
}

int bindedSocket(const Config & config) noexcept {
    auto socket = ::socket(AF_INET, SOCK_DGRAM, 0);
    auto ip = IpType::fromString(config.address);

    sockaddr_in sockaddr;
    std::memset(&sockaddr, 0, sizeof(sockaddr));

    sockaddr.sin_addr.s_addr = ip.net();
    sockaddr.sin_port = config.serverPort.net();
    sockaddr.sin_family = AF_INET;

    auto casted = reinterpret_cast<struct sockaddr *>(&sockaddr);
    int opt = 1;
    if (setsockopt(socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0)
        logError("setsockopt");

    struct timeval tv;
    tv.tv_sec = 1;
    tv.tv_usec = 0;
    if (setsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv)) < 0)
        logError("setsockopt");

    if (setsockopt(socket, SOL_SOCKET, SO_BROADCAST, &opt, sizeof(opt)) < 0)
        logError("setsockopt");

    if (bind(socket, casted, sizeof(sockaddr)) < 0) {
        logError("bind");
        close(socket);
        return -1;
    }

    logInfo(
        "Binded address: " + ip.toString() + ":" +
        std::to_string(config.serverPort));

    return socket;
}

}  // namespace dhcp
