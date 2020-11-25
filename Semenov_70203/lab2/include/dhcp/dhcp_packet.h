#pragma once

#include <optional>
#include <string>
#include <vector>

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

    NetInt<uint8_t> op;
    NetInt<uint8_t> htype;
    NetInt<uint8_t> hlen;
    NetInt<uint8_t> hops;
    NetInt<uint32_t> xid;
    NetInt<uint16_t> secs;
    NetInt<uint16_t> flags;
    NetInt<uint32_t> ciaddr;
    NetInt<uint32_t> yiaddr;
    NetInt<uint32_t> siaddr;
    NetInt<uint32_t> giaddr;
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

    IpType requestedIp() const noexcept;

    void setMessageType(MessageType type) noexcept;

    void setServerId(IpType ip) noexcept;

    IpType getServerId() const noexcept;

    void setSubnetMask(IpType ip) noexcept;

    void setDnsServer(IpType ip) noexcept;

    void setRouter(IpType ip) noexcept;

    void setBroadcast(IpType ip) noexcept;

    void setT1(NetInt<uint32_t> time) noexcept;

    void setT2(NetInt<uint32_t> time) noexcept;

    std::optional<NetInt<uint32_t>> getLeaseTime() const noexcept;

    void setLeaseTime(NetInt<uint32_t> time) noexcept;

    IpType ipAddress() const noexcept;

 private:
    std::vector<Option> mOptions;
};

}  // namespace dhcp
