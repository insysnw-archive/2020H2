#include "dhcp/ip_allocator.h"

#include "dhcp/common.h"

namespace dhcp {

IpAllocator::IpAllocator(const Range & range) noexcept : mRange{range} {}

IpType IpAllocator::allocate() noexcept {
    for (auto ip = mRange.from(); ip <= mRange.to(); ip++)
        if (!isReserved(ip) && isFree(ip)) {
            logInfo("Allocated " + ipToString(ip));
            mAllocated.push_back(ip);
            return ip;
        }

    if (!mReserved.empty()) {
        auto ip = mReserved.front();
        logInfo("Allocated " + ipToString(ip));
        mReserved.pop_back();
        return ip;
    }
    return UNDEFINED_IP;
}

auto findIp(const std::deque<IpType> & ips, IpType ip) {
    return std::find(ips.begin(), ips.end(), ip);
}

IpType IpAllocator::allocate(IpType preference) noexcept {
    if (mRange.contains(preference) && isFree(preference)) {
        logInfo("Allocated " + ipToString(preference));
        if (isReserved(preference))
            mReserved.erase(findIp(mReserved, preference));
        mAllocated.push_back(preference);
        return preference;
    }
    return allocate();
}

IpType IpAllocator::reserve() noexcept {
    for (auto ip = mRange.from(); ip <= mRange.to(); ip++)
        if (!isReserved(ip) && isFree(ip)) {
            logInfo("Reserved " + ipToString(ip));
            mReserved.push_back(ip);
            return ip;
        }
    return UNDEFINED_IP;
}

IpType IpAllocator::reserve(IpType ip) noexcept {
    if (isFree(ip)) {
        logInfo("Reserved " + ipToString(ip));
        mReserved.push_back(ip);
        return ip;
    }
    return reserve();
}

void IpAllocator::deallocate(IpType ip) noexcept {
    auto item = findIp(mAllocated, ip);
    if (item == mAllocated.end())
        return;

    logInfo("Released " + ipToString(ip));
    mAllocated.erase(item);
}

bool IpAllocator::isFree(IpType ip) const noexcept {
    return findIp(mAllocated, ip) == mAllocated.end() && mRange.contains(ip);
}

bool IpAllocator::isReserved(IpType ip) const noexcept {
    return findIp(mReserved, ip) != mReserved.end();
}

}  // namespace dhcp
