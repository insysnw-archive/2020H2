#include "dhcp/config.h"

#include <sys/socket.h>
#include <unistd.h>
#include <iostream>
#include <string>

namespace dhcp {

Config::Config(int argc, char * argv[]) noexcept {
    auto arguments = "r:i:p:d:m:h";
    int option;

    while ((option = getopt(argc, argv, arguments)) != -1) {
        switch (option) {
            case 'r': range = Range{optarg};
            case 'i': address = optarg; break;
            case 'p': port = std::stoi(optarg); break;
            case 'd': defaultLeaseTime = std::stoi(optarg); break;
            case 'm': maxLeaseTime = std::stoi(optarg); break;
            case 'h': [[fallthrough]];
            default:
                // clang-format off
                std::cout
                    << "Usage: dhcp_server [-r ip:ip] [-i addr] [-p port] [-m max] [-d default]\n"
                    << "-r      set ip range (default 192.168.0.101:192.168.0.200)\n"
                    << "-i      specify ip address (default 0.0.0.0)\n"
                    << "-p      specify port (default 67)\n"
                    << "-m      set max lease time (default 7200 sec)\n"
                    << "-d      set default lease time (default 3600 sec)" << std::endl;
                // clang-format on
                exit(0);
        }
    }
}

}  // namespace dhcp
