#pragma once

#include <string>
#include "dhcp/lease.h"
#include "dhcp/range.h"

namespace dhcp {

struct Config {
    explicit Config(int argc, char * argv[]) noexcept;

    NetInt<in_port_t> serverPort = 67;
    NetInt<in_port_t> clientPort = 68;
    std::string address = "192.168.0.2";
    std::string router = "192.168.0.1";
    std::string dnsServer = "8.8.8.8";
    std::string mask = "255.255.255.0";

    float t1 = 0.5f;
    float t2 = 0.9f;

    net32 defaultLeaseTime = 3600;
    net32 maxLeaseTime = INFINITY_TIME;
    Range range = Range{"192.168.0.101:192.168.0.200"};
};

}  // namespace dhcp
