#include "dhcp/range.h"

#include <arpa/inet.h>
#include <algorithm>
#include <string_view>

#include "dhcp/common.h"

namespace dhcp {

Range::Range() noexcept {
    mFrom = mTo = 0;
}

Range::Range(std::string_view range) noexcept {
    auto separator = ':';
    auto firstEnd = std::find(range.begin(), range.end(), separator);
    mFrom = stringToIp(std::string{range.begin(), firstEnd});
    mTo = stringToIp(std::string{firstEnd + 1, range.end()});
    if (mFrom > mTo)
        std::swap(mFrom, mTo);
}

IpType Range::from() const noexcept {
    return mFrom;
}

IpType Range::to() const noexcept {
    return mTo;
}

size_t Range::size() const noexcept {
    return mTo - mFrom + 1;
}

bool Range::contains(IpType ip) const noexcept {
    return ip >= mFrom && ip <= mTo;
}

bool Range::isValid() const noexcept {
    return mFrom != 0 && mTo != 0;
}

}  // namespace dhcp
