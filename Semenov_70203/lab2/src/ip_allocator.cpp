#include "dhcp/ip_allocator.h"

namespace dhcp {

IpAllocator::IpAllocator(const Range & range) noexcept : mRange{range} {}

std::optional<IpType> IpAllocator::allocate() noexcept {
    for (auto ip = mRange.from(); ip <= mRange.to(); ip++)
        if (isFree(ip)) {
            mAllocated.push_back(ip);
            return ip;
        }
    // no free ip addresses
    if (!mReserved.empty()) {
        auto ip = mReserved.back();
        mReserved.pop_back();
        return ip;
    }
    return std::nullopt;
}

auto findIp(const std::vector<IpType> & ips, IpType ip) {
    return std::find(ips.begin(), ips.end(), ip);
}

std::optional<IpType> IpAllocator::allocate(IpType preference) noexcept {
    if (mRange.contains(preference) && isFree(preference)) {
        mAllocated.push_back(preference);
        return preference;
    }
    if (isReserved(preference)) {
        mAllocated.push_back(preference);
        mReserved.erase(findIp(mReserved, preference));
    }
    return allocate();
}

void IpAllocator::deallocate(IpType ip) noexcept {
    mAllocated.erase(findIp(mAllocated, ip));
    mReserved.push_back(ip);
}

bool IpAllocator::isFree(IpType ip) const noexcept {
    return findIp(mAllocated, ip) == mAllocated.end();
}

bool IpAllocator::isReserved(IpType ip) const noexcept {
    return findIp(mReserved, ip) == mReserved.end();
}

}  // namespace dhcp
