#pragma once

#include <optional>
#include <vector>

#include "dhcp/range.h"

namespace dhcp {
class IpAllocator {
 public:
    explicit IpAllocator(const Range & range) noexcept;

    std::optional<IpType> allocate() noexcept;

    std::optional<IpType> allocate(IpType preference) noexcept;

    void deallocate(IpType ip) noexcept;

 private:
    bool isFree(IpType ip) const noexcept;

    bool isReserved(IpType ip) const noexcept;

 private:
    Range mRange;
    std::vector<IpType> mAllocated;
    std::vector<IpType> mReserved;
};

}  // namespace dhcp
