#include "dhcp/common.h"

#include <unistd.h>

#include <cstring>
#include <iostream>
#include <mutex>

#include "dhcp/config.h"

namespace dhcp {

void logInfo(std::string_view info, LogType type) noexcept {
    static std::mutex lMutex;
    std::lock_guard lock{lMutex};

    std::string color;
    switch (type) {
        case LogType::INFO: color = "\033[36m"; break;
        case LogType::WARNING: color = "\033[33m"; break;
        case LogType::ERRNO: color = "\033[31m"; break;
    }

    std::cout << color;
    std::cout << info;

    if (type == LogType::ERRNO)
        std::cout << ": " << strerror(errno);
    std::cout << "\033[0m" << std::endl;
}

}  // namespace dhcp
