#pragma once

#include <atomic>
#include <limits>
#include <mutex>

#include "dhcp/ip_type.h"
#include "dhcp/net_int.h"
#include "dhcp/timer.h"

namespace dhcp {

class IpAllocator;

constexpr auto INFINITY_TIME = std::numeric_limits<uint32_t>::max();

class Lease {
 public:
    Lease() noexcept;

    explicit Lease(IpType ip, net32 time, IpAllocator * allocator) noexcept;

    Lease(Lease && other) noexcept;

    ~Lease() noexcept;

    bool isActive() const noexcept;

    IpType ip() const noexcept;

    net32 remainingTime() const noexcept;

    void updateTime(net32 time) noexcept;

    void release() noexcept;

    Lease & operator=(Lease && other) noexcept;

 private:
    void assign(Lease && other) noexcept;

 private:
    IpType mIp;
    Timer mTimer;
    IpAllocator * mAllocator;
    std::atomic_bool mIsActive;
    mutable std::mutex mMutex;
};

}  // namespace dhcp
