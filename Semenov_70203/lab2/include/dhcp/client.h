#pragma once

#include <vector>

#include "dhcp/ip_allocator.h"
#include "dhcp/leased_ip.h"
#include "dhcp/timer.h"

namespace dhcp {

enum class MessageType {
    UNDEFINED = 0,
    DHCPDISCOVER = 1,
    DHCPOFFER,
    DHCPREQUEST,
    DHCPDECLINE,
    DHCPACK,
    DHCPNACK,
    DHCPRELEASE
};

struct Client {
    LeasedIp ip;
    Timer timer;
    RawType chaddr;
    MessageType message;

    Client() noexcept;

    bool operator==(const Client & other) const noexcept;
};

class ClientManager {
 public:
    ClientManager() = default;

    Client & get(const RawType & chaddr) noexcept;

    Client & operator[](const RawType & chaddr) noexcept;

    void clear() noexcept;

 private:
    std::vector<Client> mClients;
};

}  // namespace dhcp
