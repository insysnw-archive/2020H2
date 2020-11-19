#pragma once

#include <string>
#include "dhcp/range.h"

namespace dhcp {

struct Config {
    explicit Config(int argc, char * argv[]) noexcept;

    NetInt<in_port_t> port = 67;
    std::string address = "0.0.0.0";
    uint32_t defaultLeaseTime = 3600;
    uint32_t maxLeaseTime = 7200;
    Range range = Range{"192.168.0.101:192.168.0.200"};
};

}  // namespace dhcp
