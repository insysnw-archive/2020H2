#pragma once

#include <optional>
#include <string>
#include <vector>

#include "dhcp/ip_type.h"
#include "dhcp/net_int.h"

namespace dhcp {

enum class MessageType {
    UNDEFINED = 0,
    DHCPDISCOVER = 1,
    DHCPOFFER,
    DHCPREQUEST,
    DHCPDECLINE,
    DHCPACK,
    DHCPNACK,
    DHCPRELEASE,
    DHCPINFORM
};

struct Option {
    uint8_t code;
    RawType data;
};

struct DhcpPacket {
    using OptionsList = std::vector<Option>;

    net8 op;
    net8 htype;
    net8 hlen;
    net8 hops;
    net32 xid;
    net16 secs;
    net16 flags;
    IpType ciaddr;
    IpType yiaddr;
    IpType siaddr;
    IpType giaddr;
    RawType chaddr;
    RawType sname;
    RawType file;

 public:
    RawType serialize() const noexcept;

    static DhcpPacket deserialize(const RawType & bytes) noexcept;

    void print() const noexcept;

    // general option control

    Option * getOption(uint8_t code) noexcept;

    std::optional<Option> getOption(uint8_t code) const noexcept;

    void addOption(Option && option) noexcept;

    void clearOptions() noexcept;

    // specific options

    MessageType messageType() const noexcept;

    RawType clientId() const noexcept;

    std::optional<IpType> requestedIp() const noexcept;

    void setMessageType(MessageType type) noexcept;

    void setServerId(IpType ip) noexcept;

    std::optional<IpType> getServerId() const noexcept;

    void setSubnetMask(IpType ip) noexcept;

    void setDnsServer(IpType ip) noexcept;

    void setRouter(IpType ip) noexcept;

    void setBroadcast(IpType ip) noexcept;

    void setT1(net32 time) noexcept;

    void setT2(net32 time) noexcept;

    std::optional<net32> getLeaseTime() const noexcept;

    void setLeaseTime(net32 time) noexcept;

    IpType ipAddress() const noexcept;

 private:
    std::vector<Option> mOptions;
};

}  // namespace dhcp
