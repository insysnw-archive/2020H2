#include "dhcp/ip_allocator.h"

#include "dhcp/common.h"
#include "dhcp/ip_type.h"
#include "dhcp/net_int.h"

namespace dhcp {

IpAllocator::IpAllocator(const Range & range) noexcept : mRange{range} {
    mClearReservedTimer.setCallback([this]() { this->clearTemporary(); });
}

auto findIp(const IpAllocator::IpContainer & container, IpType ip) {
    return std::find(container.begin(), container.end(), ip);
}

Lease IpAllocator::allocate(net32 time) noexcept {
    std::lock_guard lock{mMutex};
    for (auto ip = mRange.from(); ip <= mRange.to(); ip++)
        if (!isReserved(ip) && isFree(ip)) {
            return doAllocate(ip, time);
        }

    if (!mTemporary.empty()) {
        auto ip = mTemporary.front();
        return doAllocate(ip, time);
    }

    if (!mReserved.empty()) {
        auto ip = mReserved.front();
        return doAllocate(ip, time);
    }

    return Lease{};
}

Lease IpAllocator::allocate(net32 time, IpType preference) noexcept {
    auto lease = tryToAllocate(time, preference);
    if (lease.isActive())
        return lease;
    return allocate(time);
}

Lease IpAllocator::tryToAllocate(net32 time, IpType ip) noexcept {
    std::lock_guard lock{mMutex};
    if (mRange.contains(ip) && isFree(ip))
        return doAllocate(ip, time);

    return Lease{};
}

std::optional<IpType> IpAllocator::reserve() noexcept {
    std::lock_guard lock{mMutex};
    for (auto ip = mRange.from(); ip <= mRange.to(); ++ip)
        if (!isReserved(ip) && isFree(ip))
            return doReserve(ip);

    return UNDEFINED_IP;
}

std::optional<IpType> IpAllocator::reserve(IpType ip) noexcept {
    std::lock_guard lock{mMutex};
    if (isFree(ip))
        return doReserve(ip);
    return reserve();
}

void IpAllocator::deallocate(IpType ip) noexcept {
    std::lock_guard lock{mMutex};
    auto ipAllocated = findIp(mAllocated, ip);
    auto ipReserved = findIp(mReserved, ip);
    auto ipTemporary = findIp(mTemporary, ip);
    auto ips = ip.toString();

    if (ipTemporary != mTemporary.end()) {
        logInfo("Temporary ip " + ips + " is released");
        mTemporary.erase(ipTemporary);
    }

    if (ipReserved != mReserved.end()) {
        logInfo("Ip " + ips + " is back from reservation");
        mReserved.erase(ipReserved);
    }

    if (ipAllocated != mAllocated.end()) {
        logInfo("Released " + ips);
        mAllocated.erase(ipAllocated);
        logInfo("Ip " + ips + " is reserved for this client");
        mReserved.push_back(ip);
    }
}

bool IpAllocator::isFree(IpType ip) const noexcept {
    std::lock_guard lock{mMutex};
    return findIp(mAllocated, ip) == mAllocated.end() && mRange.contains(ip);
}

bool IpAllocator::isReserved(IpType ip) const noexcept {
    std::lock_guard lock{mMutex};
    return findIp(mReserved, ip) != mReserved.end() ||
           findIp(mTemporary, ip) != mTemporary.end();
}

IpType IpAllocator::doReserve(IpType ip) noexcept {
    std::lock_guard lock{mMutex};
    logInfo("Temporary reserved " + ip.toString());
    mTemporary.push_back(ip);
    mClearReservedTimer.start(RESERVE_TIME);
    return ip;
}

Lease IpAllocator::doAllocate(IpType ip, net32 time) noexcept {
    std::lock_guard lock{mMutex};
    deallocate(ip);
    mAllocated.push_back(ip);
    logInfo("Allocated " + ip.toString() + " for client");
    return Lease{ip, time, this};
}

void IpAllocator::clearTemporary() noexcept {
    std::lock_guard lock{mMutex};
    for (auto ip : mTemporary)
        deallocate(ip);
}

}  // namespace dhcp
