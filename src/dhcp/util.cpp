//
// Created by Roman Svechnikov on 07.12.2020.
//

#include "util.h"

std::array<uint8_t, 6> util::parseMac(const std::string &input) {
    if (input.length() == 12) {
        std::array<uint8_t, 6> result{};
        std::sscanf(input.c_str(), "%2hhx%2hhx%2hhx%2hhx%2hhx%2hhx",
                    &result[0], &result[1], &result[2], &result[3], &result[4], &result[5]);
        return result;
    } else if (input.length() == 12 + 5) {
        std::array<uint8_t, 6> result{};
        std::sscanf(input.c_str(), "%hhx:%hhx:%hhx:%hhx:%hhx:%hhx",
                    &result[0], &result[1], &result[2], &result[3], &result[4], &result[5]);
        return result;
    } else {
        throw std::invalid_argument{"Invalid length of the MAC address"};
    }
}
