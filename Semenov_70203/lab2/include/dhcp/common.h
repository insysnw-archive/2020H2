#pragma once

#include <arpa/inet.h>
#include <optional>
#include <string>
#include <string_view>

#include "dhcp/net_int.h"

namespace dhcp {

struct Config;

void logError(std::string_view source) noexcept;

void logInfo(std::string_view info) noexcept;

std::string ipToString(IpType ip) noexcept;

IpType stringToIp(std::string_view str) noexcept;

int bindedSocket(const Config & setup) noexcept;

}  // namespace dhcp
