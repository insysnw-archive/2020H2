#pragma once

#include <atomic>
#include <mutex>
#include <thread>

#include "dhcp/client.h"
#include "dhcp/common.h"
#include "dhcp/config.h"
#include "dhcp/dhcp_packet.h"
#include "dhcp/ip_allocator.h"

namespace dhcp {

class DhcpServer {
 public:
    explicit DhcpServer(const Config & config) noexcept;

    ~DhcpServer() noexcept;

    void stop() noexcept;

 private:
    std::optional<DhcpPacket> receivePacket(int socket) const noexcept;

    void sendPacket(const DhcpPacket * packet) const noexcept;

    void onDiscover(DhcpPacket * packet) noexcept;

    void onRequest(DhcpPacket * packet) noexcept;

    void onRelease(DhcpPacket * packet) noexcept;

    void onDecline(DhcpPacket * packet) noexcept;

    void onInform(DhcpPacket * packet) noexcept;

    void ack(DhcpPacket * packet) noexcept;

    void nack(DhcpPacket * packet) noexcept;

    void offer(DhcpPacket * packet) noexcept;

    void threadStart(int socket) noexcept;

    void preparePacket(DhcpPacket * packet) noexcept;

 private:
    int mSocket;
    int mBroadcastSocket;
    std::thread mThreadIp;
    std::thread mThreadBroadcast;
    std::mutex mMutex;
    std::atomic_bool mStopped;

    IpType mId;
    Config mConfig;
    ClientManager mClients;
    IpAllocator mAllocator;
};

}  // namespace dhcp
