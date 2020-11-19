#include "dhcp/dhcp_server.h"

#include <sys/socket.h>
#include <unistd.h>
#include <array>

#include "dhcp/common.h"
#include "dhcp/config.h"
#include "dhcp/dhcp_package.h"

namespace dhcp {

DhcpServer::DhcpServer(const Config & config) noexcept
    : mStopped{false}, mAllocator{config.range} {
    mSocket = bindedSocket(config);
    logInfo(
        "Ip from " + ipToString(config.range.from()) + " to " +
        ipToString(config.range.to()));

    if (mSocket >= 0)
        mThread = std::thread{&DhcpServer::threadStart, this};
}

DhcpServer::~DhcpServer() noexcept {
    if (mThread.joinable())
        mThread.join();
}

void DhcpServer::threadStart() noexcept {
    while (!mStopped) {
        std::array<char, 512> buffer;
        sockaddr_in address;
        socklen_t socklen;

        auto bytes = recvfrom(
            mSocket, buffer.begin(), buffer.size(), MSG_WAITALL,
            reinterpret_cast<sockaddr *>(&address), &socklen);

        if (bytes <= 0)
            continue;

        RawType raw{buffer.data(), static_cast<size_t>(bytes)};
        auto package = DhcpPackage::deserialize(raw);
        package.print();
    }
    close(mSocket);
}

void DhcpServer::stop() noexcept {
    mStopped.store(true);
}

}  // namespace dhcp
