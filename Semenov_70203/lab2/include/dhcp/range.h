#pragma once

#include <string_view>

#include "dhcp/net_int.h"

namespace dhcp {

class Range {
 public:
    Range() noexcept;

    explicit Range(std::string_view range) noexcept;

    bool isValid() const noexcept;

    IpType from() const noexcept;

    IpType to() const noexcept;

    size_t size() const noexcept;

    bool contains(IpType ip) const noexcept;

 private:
    IpType mFrom;
    IpType mTo;
};

}  // namespace dhcp
