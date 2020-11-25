#pragma once

#include <arpa/inet.h>
#include <algorithm>
#include <string>

namespace dhcp {

template <class Type>
class NetInt;

using IpType = NetInt<in_addr_t>;
constexpr in_addr_t UNDEFINED_IP = 0;
using RawType = std::string;

template <class Type>
class NetInt {
 public:
    constexpr static size_t size = sizeof(Type);

 public:
    NetInt() noexcept : mValue{0} {}

    // implicit
    NetInt(Type value) noexcept : mValue{value} {}

    // implicit
    NetInt(const RawType & raw) : NetInt{raw.begin()} {}

    static NetInt fromNet(Type value) noexcept {
        return NetInt{NetInt{value}.net()};
    }

    explicit NetInt(RawType::const_iterator raw) noexcept {
        fromRaw(raw);
    }

    NetInt & fromRaw(RawType::const_iterator raw) noexcept {
        Type netValue;
        std::copy(raw, raw + size, reinterpret_cast<char *>(&netValue));

        if constexpr (sizeof(Type) == 4)
            mValue = htonl(netValue);
        else if (sizeof(Type) == 2)
            mValue = htons(netValue);
        else
            mValue = netValue;

        return *this;
    }

    RawType toRaw() const noexcept {
        RawType raw;
        auto netValue = net();
        raw.append(reinterpret_cast<char *>(&netValue), size);
        return raw;
    }

    Type net() const noexcept {
        if constexpr (sizeof(Type) == 4)
            return ntohl(mValue);
        else if (sizeof(Type) == 2)
            return ntohs(mValue);
        return mValue;
    }

    operator Type() const noexcept {
        return mValue;
    }

    NetInt & operator++() noexcept {
        mValue += 1;
        return *this;
    }

    NetInt operator++(int) noexcept {
        NetInt saved = *this;
        mValue += 1;
        return saved;
    }

 private:
    Type mValue;
};

}  // namespace dhcp
