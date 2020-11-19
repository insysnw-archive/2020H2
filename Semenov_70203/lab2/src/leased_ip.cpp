#include "dhcp/leased_ip.h"

#include "dhcp/ip_allocator.h"

namespace dhcp {

LeasedIp::LeasedIp() noexcept : mAllocator{nullptr}, mIp{0} {}

LeasedIp::LeasedIp(IpAllocator * allocator, IpType preference) noexcept
    : mAllocator{allocator} {
    auto optionalIp = mAllocator->allocate(preference);
    if (optionalIp.has_value())
        mIp = *optionalIp;
    else
        mIp = 0;
}

LeasedIp::~LeasedIp() noexcept {
    onTimer();
}

void LeasedIp::onTimer() noexcept {
    if (mAllocator)
        mAllocator->deallocate(mIp);
    mIp = 0;
}

LeasedIp::operator IpType() {
    return mIp;
}

}  // namespace dhcp
