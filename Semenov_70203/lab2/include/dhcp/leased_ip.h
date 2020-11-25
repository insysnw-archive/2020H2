#pragma once

#include "dhcp/net_int.h"
#include "dhcp/timer.h"

namespace dhcp {

class IpAllocator;

class LeasedIp : public ITimerListener {
 public:
    LeasedIp() noexcept;

    LeasedIp(const LeasedIp &) = delete;

    LeasedIp(LeasedIp && other) noexcept;

    explicit LeasedIp(
        IpAllocator * allocator,
        IpType preference = IpType{}) noexcept;

    ~LeasedIp() noexcept;

    operator IpType() const;

    bool operator==(IpType ip) const;

    bool operator!=(IpType ip) const;

    LeasedIp & operator=(const LeasedIp & other) = delete;

    LeasedIp & operator=(LeasedIp && other) noexcept;

 private:
    void release() noexcept;

    void onTimer() noexcept override;

    void swap(LeasedIp && other) noexcept;

 private:
    IpAllocator * mAllocator;
    IpType mIp;
};

}  // namespace dhcp
