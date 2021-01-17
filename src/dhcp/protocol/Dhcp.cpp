//
// Created by Roman Svechnikov on 10.12.2020.
//

#include <cstdio>
#include "Dhcp.h"

std::pair<char *, size_t> Dhcp::serialize() const {
    size_t dataLength = sizeof(DhcpBase);

    std::vector<uint8_t> rawOptions {};
    for (auto [type, body] : options) {
        rawOptions.push_back(type);
        rawOptions.push_back(body.size());
        rawOptions.insert(rawOptions.end(), body.begin(), body.end());
    }
    if (!options.empty()) {
        rawOptions.push_back(OPTION_END);
    }

    if (!rawOptions.empty()) {
        dataLength += sizeof(DHCP_MAGIC_COOKIE) + rawOptions.size();
    }

    auto *data = static_cast<char *>(std::malloc(dataLength));
    memcpy(data, dhcpBase, sizeof(DhcpBase));

    if (!rawOptions.empty()) {
        memcpy(data + sizeof(DhcpBase), &DHCP_MAGIC_COOKIE, sizeof(DHCP_MAGIC_COOKIE));
        memcpy(data + sizeof(DhcpBase) + sizeof(DHCP_MAGIC_COOKIE), rawOptions.data(), rawOptions.size());
    }

    return std::make_pair(data, dataLength);
}

Dhcp::Dhcp() : dhcpBase(new DhcpBase) {
}
