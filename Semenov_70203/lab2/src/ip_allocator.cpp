#include "dhcp/ip_allocator.h"

#include "dhcp/common.h"
#include "dhcp/net_int.h"

namespace dhcp {

IpAllocator::IpAllocator(const Range & range) noexcept : mRange{range} {}

auto findIp(const std::deque<IpType> & ips, IpType ip) {
    return std::find(ips.begin(), ips.end(), ip);
}

Lease IpAllocator::allocate(net32 time) noexcept {
    std::lock_guard lock{mMutex};
    for (auto ip = mRange.from(); ip <= mRange.to(); ip++)
        if (!isReserved(ip) && isFree(ip)) {
            logInfo("Allocated " + ip.toString());
            mAllocated.push_back(ip);
            return Lease{ip, time, this};
        }

    if (!mReserved.empty()) {
        auto ip = mReserved.front();
        logInfo("Allocated " + ip.toString());
        mReserved.pop_back();
        return Lease{ip, time, this};
    }

    Lease inactive;
    return inactive;
}

Lease IpAllocator::allocate(net32 time, IpType preference) noexcept {
    std::lock_guard lock{mMutex};
    if (mRange.contains(preference) && isFree(preference)) {
        logInfo("Allocated " + preference.toString());
        auto reserved = isReserved(preference);
        if (reserved)
            mReserved.erase(findIp(mReserved, preference));
        mAllocated.push_back(preference);
        return Lease{preference, time, this};
    }
    return allocate(time);
}

IpAllocator::Reserve IpAllocator::reserve() noexcept {
    std::lock_guard lock{mMutex};
    for (auto ip = mRange.from(); ip <= mRange.to(); ip++)
        if (!isReserved(ip) && isFree(ip)) {
            logInfo("Reserved " + ip.toString());
            mReserved.push_back(ip);
            Reserve reserve{ip, RESERVE_TIME, this};
            return reserve;
        }
    return Reserve{};
}

IpAllocator::Reserve IpAllocator::reserve(IpType ip) noexcept {
    std::lock_guard lock{mMutex};
    if (isFree(ip)) {
        logInfo("Reserved " + ip.toString());
        mReserved.push_back(ip);
        Reserve reserve{ip, RESERVE_TIME, this};
        return reserve;
    }
    return reserve();
}

void IpAllocator::deallocate(IpType ip) noexcept {
    std::lock_guard lock{mMutex};
    auto itemAllocated = findIp(mAllocated, ip);
    auto itemReserved = findIp(mReserved, ip);

    if (itemAllocated != mAllocated.end()) {
        logInfo("Released " + ip.toString());
        mAllocated.erase(itemAllocated);
        mReserved.push_back(*itemAllocated);
    } else if (itemReserved != mReserved.end()) {
        logInfo("Ip " + ip.toString() + " is back from reservation");
        mReserved.erase(itemReserved);
    }
}

bool IpAllocator::isFree(IpType ip) const noexcept {
    std::lock_guard lock{mMutex};
    return findIp(mAllocated, ip) == mAllocated.end() && mRange.contains(ip);
}

bool IpAllocator::isReserved(IpType ip) const noexcept {
    std::lock_guard lock{mMutex};
    return findIp(mReserved, ip) != mReserved.end();
}

}  // namespace dhcp
