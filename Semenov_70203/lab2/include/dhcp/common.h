#pragma once

#include <arpa/inet.h>
#include <optional>
#include <string>
#include <string_view>

#include "dhcp/net_int.h"

namespace dhcp {

enum class LogType { INFO, WARNING, ERRNO };

void logInfo(std::string_view info, LogType type = LogType::INFO) noexcept;

}  // namespace dhcp
