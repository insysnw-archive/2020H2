#pragma once

#include <deque>
#include <mutex>

#include "dhcp/lease.h"
#include "dhcp/net_int.h"
#include "dhcp/range.h"

namespace dhcp {

class IpAllocator {
 public:
    using IpContainer = std::deque<IpType>;
    static constexpr uint32_t RESERVE_TIME = 30;

 public:
    explicit IpAllocator(const Range & range) noexcept;

    Lease allocate(net32 time) noexcept;

    Lease allocate(net32 time, IpType preference) noexcept;

    Lease tryToAllocate(net32 time, IpType ip) noexcept;

    std::optional<IpType> reserve() noexcept;

    std::optional<IpType> reserve(IpType preference) noexcept;

    void deallocate(IpType ip) noexcept;

    bool isFree(IpType ip) const noexcept;

 private:
    bool isReserved(IpType ip) const noexcept;

    IpType doReserve(IpType ip) noexcept;

    Lease doAllocate(IpType ip, net32 time) noexcept;

    void clearTemporary() noexcept;

 private:
    Range mRange;
    mutable std::recursive_mutex mMutex;

    IpContainer mAllocated;
    IpContainer mReserved;

    IpContainer mTemporary;
    Timer mClearReservedTimer;
};

}  // namespace dhcp
