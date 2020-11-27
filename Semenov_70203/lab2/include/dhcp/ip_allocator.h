#pragma once

#include <deque>
#include <mutex>

#include "dhcp/lease.h"
#include "dhcp/net_int.h"
#include "dhcp/range.h"

namespace dhcp {

class IpAllocator {
 public:
    using Reserve = Lease;
    static constexpr uint32_t RESERVE_TIME = 30;

 public:
    explicit IpAllocator(const Range & range) noexcept;

    Lease allocate(net32 time) noexcept;

    Lease allocate(net32 time, IpType preference) noexcept;

    Reserve reserve() noexcept;

    Reserve reserve(IpType preference) noexcept;

    void deallocate(IpType ip) noexcept;

    bool isFree(IpType ip) const noexcept;

 private:
    bool isReserved(IpType ip) const noexcept;

 private:
    Range mRange;
    mutable std::recursive_mutex mMutex;

    std::deque<IpType> mAllocated;
    std::deque<IpType> mReserved;
};

}  // namespace dhcp
