#pragma once

#include <string>

#include "dhcp/log.h"

namespace dhcp {

template <class Type>
class NetInt;

using RawType = std::string;

template <class Type>
class NetInt {
 public:
    using BaseType = Type;
    constexpr static size_t size = sizeof(BaseType);

 public:
    NetInt() noexcept;

    // implicit
    NetInt(BaseType hostValue) noexcept;

    virtual ~NetInt() = default;

    static NetInt fromNet(BaseType netValue) noexcept;

    static NetInt fromRaw(RawType::const_iterator raw) noexcept;

    static NetInt fromRaw(const RawType & raw) noexcept;

    void assign(RawType::const_iterator raw) noexcept;

    RawType toRaw() const noexcept;

    BaseType net() const noexcept;

    operator BaseType() const noexcept;

    NetInt & operator++() noexcept;

    NetInt operator++(int) noexcept;

 private:
    BaseType mValue;
};

using net32 = NetInt<uint32_t>;
using net16 = NetInt<uint16_t>;
using net8 = NetInt<uint8_t>;

}  // namespace dhcp
