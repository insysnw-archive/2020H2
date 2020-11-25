#pragma once

#include <deque>

#include "dhcp/range.h"

namespace dhcp {
class IpAllocator {
 public:
    explicit IpAllocator(const Range & range) noexcept;

    IpType allocate() noexcept;

    IpType allocate(IpType preference) noexcept;

    IpType reserve() noexcept;

    IpType reserve(IpType preference) noexcept;

    void deallocate(IpType ip) noexcept;

    bool isFree(IpType ip) const noexcept;

 private:
    bool isReserved(IpType ip) const noexcept;

 private:
    Range mRange;
    std::deque<IpType> mAllocated;
    std::deque<IpType> mReserved;
};

}  // namespace dhcp
