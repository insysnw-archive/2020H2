#include "dhcp/leased_ip.h"

#include "dhcp/ip_allocator.h"

namespace dhcp {

LeasedIp::LeasedIp() noexcept : mAllocator{nullptr} {}

LeasedIp::LeasedIp(LeasedIp && other) noexcept {
    swap(std::move(other));
}

LeasedIp::LeasedIp(IpAllocator * allocator, IpType preference) noexcept
    : mAllocator{allocator} {
    mIp = mAllocator->allocate(preference);
}

LeasedIp::~LeasedIp() noexcept {
    release();
}

void LeasedIp::release() noexcept {
    if (mAllocator)
        mAllocator->deallocate(mIp);
    mIp = UNDEFINED_IP;
}

void LeasedIp::onTimer() noexcept {
    release();
}

bool LeasedIp::operator==(IpType ip) const {
    return mIp == ip;
}

bool LeasedIp::operator!=(IpType ip) const {
    return mIp != ip;
}

LeasedIp::operator IpType() const {
    return mIp;
}

LeasedIp & LeasedIp::operator=(LeasedIp && other) noexcept {
    swap(std::move(other));
    return *this;
}

void LeasedIp::swap(LeasedIp && other) noexcept {
    mAllocator = other.mAllocator;
    mIp = other.mIp;
    other.mAllocator = nullptr;
    other.mIp = UNDEFINED_IP;
}

}  // namespace dhcp
