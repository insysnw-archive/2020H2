//
// Created by Roman Svechnikov on 17.01.2021.
//

#ifndef NETLAB2_DHCPPACKETS_H
#define NETLAB2_DHCPPACKETS_H

#include <array>
#include "Dhcp.h"

Dhcp buildDhcpDiscoveryPacket(uint32_t xid, std::array<uint8_t, 6> mac);

Dhcp buildDhcpRequestPacket(
        uint32_t xid, std::array<uint8_t, 6> mac,
        std::array<uint8_t, 4> selectedIp, std::array<uint8_t, 4> selectedServer
);

Dhcp readDhcpPacket(const char *data, size_t length);

#endif //NETLAB2_DHCPPACKETS_H
