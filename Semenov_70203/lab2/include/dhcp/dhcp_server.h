#pragma once

#include <netinet/in.h>
#include <vector>

#include "looped_thread.h"

class EndpointSetup;

namespace dhcp {

class DhcpServer : public LoopedThread {
 public:
    using AddressPool = std::vector<sockaddr_in>;

 public:
    explicit DhcpServer(const EndpointSetup & setup) noexcept;

    ~DhcpServer() noexcept;

    void onThreadStart() noexcept override;

    void threadStep() noexcept override;

    void onThreadFinish() noexcept override;

    void onStop() noexcept override;

 private:
    int mSocket;
    AddressPool mAddresses;
};

} // namespace dhcp
