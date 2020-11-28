#include "dhcp/net_int.h"

#include <arpa/inet.h>

namespace dhcp {

template <class T>
NetInt<T>::NetInt() noexcept : mValue{0} {};

template <class T>
NetInt<T>::NetInt(BaseType hostValue) noexcept : mValue{hostValue} {};

template <class T>
NetInt<T> NetInt<T>::fromNet(BaseType netValue) noexcept {
    if constexpr (size == 4)
        return ntohl(netValue);
    else if (size == 2)
        return ntohs(netValue);
    else
        return netValue;
}

template <class T>
NetInt<T> NetInt<T>::fromRaw(RawType::const_iterator raw) noexcept {
    BaseType netValue;
    std::copy(raw, raw + size, reinterpret_cast<char *>(&netValue));
    return fromNet(netValue);
}

template <class T>
NetInt<T> NetInt<T>::fromRaw(const RawType & raw) noexcept {
    return fromRaw(raw.cbegin());
}

template <class T>
void NetInt<T>::assign(RawType::const_iterator raw) noexcept {
    mValue = NetInt::fromRaw(raw).mValue;
}

template <class T>
RawType NetInt<T>::toRaw() const noexcept {
    RawType raw;
    auto netValue = net();
    raw.append(reinterpret_cast<char *>(&netValue), size);
    return raw;
}

template <class T>
typename NetInt<T>::BaseType NetInt<T>::net() const noexcept {
    if constexpr (size == 4)
        return htonl(mValue);
    else if (size == 2)
        return htons(mValue);
    return mValue;
}

template <class T>
NetInt<T>::operator BaseType() const noexcept {
    return mValue;
}

template <class T>
NetInt<T> & NetInt<T>::operator++() noexcept {
    mValue += 1;
    return *this;
}

template <class T>
NetInt<T> NetInt<T>::operator++(int) noexcept {
    NetInt saved = *this;
    mValue += 1;
    return saved;
}

template class NetInt<uint32_t>;
template class NetInt<uint16_t>;
template class NetInt<uint8_t>;
template class NetInt<bool>;

}  // namespace dhcp
