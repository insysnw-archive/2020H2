#pragma once

#include <string_view>

#include "dhcp/net_int.h"

namespace dhcp {

enum class LogType { INFO, WARNING, ERRNO };

void log(std::string_view info, LogType type = LogType::INFO) noexcept;

}  // namespace dhcp
