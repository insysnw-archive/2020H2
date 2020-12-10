//
// Created by Roman Svechnikov on 07.12.2020.
//

#ifndef NETLAB2_UTIL_H
#define NETLAB2_UTIL_H

#include <array>
#include <stdexcept>
#include <cstdint>
#include <string>

namespace util {
    std::array<uint8_t, 6> parseMac(const std::string &input);
}

#endif //NETLAB2_UTIL_H
