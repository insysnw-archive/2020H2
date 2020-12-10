//
// Created by Roman Svechnikov on 10.12.2020.
//

#include <cstdio>
#include "Dhcp.h"

std::pair<char *, size_t> Dhcp::serialize() const {
    size_t dataLength = sizeof(DhcpBase);
    if (optionsSize > 0) {
        dataLength += sizeof(DHCP_MAGIC_COOKIE) + optionsSize;
    }
    std::printf("Packet size: %d", dataLength);
    auto *data = static_cast<char *>(std::malloc(dataLength));
    memcpy(data, dhcpBase, sizeof(DhcpBase));
    if (optionsSize > 0) {
        memcpy(data + sizeof(DhcpBase), &DHCP_MAGIC_COOKIE, sizeof(DHCP_MAGIC_COOKIE));
        memcpy(data + sizeof(DhcpBase) + sizeof(DHCP_MAGIC_COOKIE), options, optionsSize);
    }

    return std::make_pair(data, dataLength);
}

Dhcp::Dhcp() : dhcpBase(new DhcpBase) {
}
