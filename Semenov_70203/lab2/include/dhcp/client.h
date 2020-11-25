#pragma once

#include <optional>
#include <vector>

#include "dhcp/dhcp_packet.h"
#include "dhcp/ip_allocator.h"
#include "dhcp/leased_ip.h"
#include "dhcp/timer.h"

namespace dhcp {

class Client {
 public:
    explicit Client(const RawType & id) noexcept;

    bool operator==(const Client & other) const noexcept;

    RawType id() const noexcept;

 public:
    LeasedIp ip;
    Timer timer;
    MessageType lastMessageType;
    NetInt<uint32_t> xid;

 private:
    RawType mId;
};

class ClientManager {
 public:
    ClientManager() = default;

    Client * get(const RawType & id) noexcept;

    Client * getOrNew(const RawType & id) noexcept;

    void clear() noexcept;

    bool has(const RawType & id) const noexcept;

 private:
    std::vector<Client> mClients;
};

}  // namespace dhcp
