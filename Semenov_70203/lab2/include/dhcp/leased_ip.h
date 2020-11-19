#pragma once

#include "dhcp/net_int.h"
#include "dhcp/timer.h"

namespace dhcp {

class IpAllocator;

class LeasedIp : public ITimerListener {
 public:
    LeasedIp() noexcept;

    explicit LeasedIp(IpAllocator * allocator, IpType preference) noexcept;

    ~LeasedIp() noexcept;

    operator IpType();

 private:
    void onTimer() noexcept override;

 private:
    IpAllocator * mAllocator;
    IpType mIp;
};

}  // namespace dhcp
