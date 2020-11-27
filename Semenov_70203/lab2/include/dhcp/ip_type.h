#pragma once

#include <arpa/inet.h>
#include <string>

#include "dhcp/net_int.h"

namespace dhcp {

constexpr auto UNDEFINED_IP = std::nullopt;

class IpType : public NetInt<in_addr_t> {
 public:
    using BaseType = in_addr_t;
    constexpr static size_t size = sizeof(BaseType);

 public:
    using NetInt::NetInt;

    // implicit
    IpType(NetInt netInt) noexcept;

    static IpType fromNet(in_addr_t address) noexcept;

    static IpType fromRaw(const RawType & raw) noexcept;

    static IpType fromRaw(RawType::const_iterator raw) noexcept;

    static IpType fromString(const std::string & str) noexcept;

    std::string toString() const noexcept;
};

}  // namespace dhcp
