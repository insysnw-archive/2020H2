#pragma once

#include <vector>

#include "dhcp/dhcp_packet.h"
#include "dhcp/lease.h"

namespace dhcp {

class Client {
 public:
    explicit Client(const RawType & id) noexcept;

    bool operator==(const Client & other) const noexcept;

    RawType id() const noexcept;

 public:
    Lease lease;
    MessageType lastMessageType;
    net32 xid;

 private:
    RawType mId;
};

class ClientManager {
 public:
    ClientManager() = default;

    Client * get(const RawType & id) noexcept;

    Client * newClient(const RawType & id) noexcept;

    void clear() noexcept;

    bool has(const RawType & id) const noexcept;

 private:
    std::vector<Client> mClients;
};

}  // namespace dhcp
