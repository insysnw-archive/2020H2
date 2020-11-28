#include "dhcp/config.h"

#include <unistd.h>
#include <iostream>
#include <string>

namespace dhcp {

net32 timeOrInfinite(std::string_view optarg) {
    if (optarg == "inf" || optarg == "infinity")
        return INFINITY_TIME;

    return std::stoi(optarg.data());
}

Config::Config(int argc, char * argv[]) noexcept {
    auto arguments = "i:g:k:b:d:r:p:m:t:h";
    int option;

    while ((option = getopt(argc, argv, arguments)) != -1) {
        switch (option) {
            case 'i': address = optarg; break;
            case 'g': router = optarg; break;
            case 'k': mask = optarg; break;
            case 'b': broadcast = optarg; break;
            case 'd': dnsServer = optarg; break;
            case 'r': range = Range{optarg}; break;
            case 'p': serverPort = std::stoi(optarg); break;
            case 'm': maxLeaseTime = timeOrInfinite(optarg); break;
            case 't': defaultLeaseTime = timeOrInfinite(optarg); break;
            case 'h': [[fallthrough]];
            default:
                // clang-format off
                std::cout
                    << "Usage: dhcp_server <-i addr> <-g addr> <-k mask> [-d addr] \n"
                    << "       [-r ip:ip] [-p port] [-m max] [-t lease]\n\n"
                    << "  -i      server ip address\n"
                    << "  -g      gateway ip address (default 192.168.0.1)\n"
                    << "  -k      subnet mask (default 255.255.255.0)\n"
                    << "  -b      broadcast address (default 255.255.255.255)\n"
                    << "  -d      dns server\n"
                    << "  -r      ip range (default 192.168.0.101:192.168.0.200)\n"
                    << "  -p      server port (default 67)\n"
                    << "  -m      max lease time (default INFINITY)\n"
                    << "  -t      default lease time (default 3600 sec)" << std::endl;
                // clang-format on
                exit(0);
        }
    }

    if (address.empty()) {
        log("You must set server's ip address", LogType::WARNING);
        exit(EXIT_FAILURE);
    }

    if (mask.empty())
        log("You should set subnet mask", LogType::WARNING);

    if (router.empty())
        log("You should set gateway ip address", LogType::WARNING);

    if (dnsServer.empty())
        log("You should set dns server's ip address", LogType::WARNING);
}

}  // namespace dhcp
