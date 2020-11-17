#include "dhcp/dhcp_server.h"

#include <sys/socket.h>
#include <unistd.h>
#include <array>
#include <iostream>

#include "dhcp/dhcp_package.h"
#include "setup.h"
#include "utils.h"

namespace dhcp {

DhcpServer::DhcpServer(const EndpointSetup & setup) noexcept {
    auto response = bindedSocket(setup.connection);
    mSocket = response.socket;
}

DhcpServer::~DhcpServer() noexcept {
    MANUAL_FINISH
}

void DhcpServer::onThreadStart() noexcept {
    if (mSocket < 0)
        stop();
}

void DhcpServer::threadStep() noexcept {
    std::array<char, 512> buffer;
    sockaddr_in address;
    socklen_t socklen;

    auto bytes = recvfrom(
        mSocket, buffer.begin(), buffer.size(), MSG_WAITALL,
        reinterpret_cast<sockaddr *>(&address), &socklen);

    RawType raw{buffer.data(), static_cast<size_t>(bytes)};
    auto package = DhcpPackage::deserialize(raw);
    package.print();
    stop();
}

void DhcpServer::onThreadFinish() noexcept {
    if (mSocket >= 0)
        close(mSocket);
}

void DhcpServer::onStop() noexcept {
    if (mSocket >= 0)
        shutdown(mSocket, SHUT_RD);
}

}  // namespace dhcp
