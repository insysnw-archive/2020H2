#include "dhcp/ip_type.h"

#include <cstring>

#include "dhcp/log.h"

namespace dhcp {

IpType::IpType(NetInt netInt) noexcept : NetInt{netInt} {}

in_addr initInAddr() noexcept {
    in_addr inaddr;
    std::memset(&inaddr, 0, sizeof(inaddr));
    return inaddr;
}

IpType IpType::fromNet(in_addr_t address) noexcept {
    return NetInt::fromNet(address);
}

IpType IpType::fromRaw(const RawType & raw) noexcept {
    return NetInt::fromRaw(raw);
}

IpType IpType::fromRaw(RawType::const_iterator raw) noexcept {
    return NetInt::fromRaw(raw);
}

IpType IpType::fromString(const std::string & str) noexcept {
    auto inaddr = initInAddr();
    if (inet_pton(AF_INET, str.data(), &inaddr) <= 0) {
        log("Cannot convert string to ip", LogType::ERRNO);
        return 0;
    }

    return IpType::fromNet(inaddr.s_addr);
}

std::string IpType::toString() const noexcept {
    char buffer[INET_ADDRSTRLEN];
    auto inaddr = initInAddr();
    inaddr.s_addr = this->net();

    if (!inet_ntop(AF_INET, &inaddr, buffer, sizeof(buffer)))
        return "";
    return std::string{buffer};
}

}  // namespace dhcp
