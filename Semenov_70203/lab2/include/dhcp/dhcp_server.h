#pragma once

#include <atomic>
#include <thread>
#include "dhcp/client.h"
#include "dhcp/ip_allocator.h"

namespace dhcp {

class Config;

class DhcpServer {
 public:
    explicit DhcpServer(const Config & config) noexcept;

    ~DhcpServer() noexcept;

    void receive() const noexcept;

    void stop() noexcept;

 private:
    void threadStart() noexcept;

 private:
    int mSocket;
    std::thread mThread;
    std::atomic_bool mStopped;

    ClientManager mManager;
    IpAllocator mAllocator;
};

}  // namespace dhcp
