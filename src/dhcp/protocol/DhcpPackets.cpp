//
// Created by Roman Svechnikov on 17.01.2021.
//

#include "DhcpPackets.h"

Dhcp buildDhcpDiscoveryPacket(uint32_t xid, std::array<uint8_t, 6> mac) {
    Dhcp packet{};

    packet->op = DHCP_BOOTREQUEST;
    packet->htype = HARDWARE_TYPE_ETHERNET_10;
    packet->hlen = mac.size();
    packet->hops = 0;
    packet->xid = xid;
    packet->secs = 0;
    packet->flags = 0b1000000000000000;
    packet->ciaddr = 0;
    packet->yiaddr = 0;
    packet->siaddr = 0;
    packet->giaddr = 0;
    memcpy(packet->chaddr, mac.data(), mac.size());

    options_t options{
            {OPTION_MESSAGE_TYPE, {MESSAGE_TYPE_DISCOVER}}
    };
    packet.options = options;

    return packet;
}

Dhcp buildDhcpRequestPacket(
        uint32_t xid, std::array<uint8_t, 6> mac,
        std::array<uint8_t, 4> selectedIp, std::array<uint8_t, 4> selectedServer
) {
    Dhcp packet{};

    packet->op = DHCP_BOOTREQUEST;
    packet->htype = HARDWARE_TYPE_ETHERNET_10;
    packet->hlen = mac.size();
    packet->hops = 0;
    packet->xid = xid;
    packet->secs = 0;
    packet->flags = 0b1000000000000000;
    packet->ciaddr = 0;
    packet->yiaddr = 0;
    packet->siaddr = 0;
    packet->giaddr = 0;
    memcpy(packet->chaddr, mac.data(), mac.size());
    options_t options{
            {OPTION_MESSAGE_TYPE, {MESSAGE_TYPE_REQUEST}},
            {OPTION_REQUESTED_IP, {selectedIp[3], selectedIp[2], selectedIp[1], selectedIp[0]}},
            {OPTION_SERVER_IDENTIFIER, {selectedServer[3], selectedServer[2], selectedServer[1], selectedServer[0]}}
    };

    packet.options = options;

    return packet;
}

Dhcp readDhcpPacket(const char *data, size_t length) {
    Dhcp packet{};
    memcpy(packet.dhcpBase, data, sizeof(DhcpBase));
    data += sizeof(DhcpBase);
    uint32_t magic;
    memcpy(&magic, data, sizeof(magic));
    if (magic == DHCP_MAGIC_COOKIE) {
        data += sizeof(magic);
        uint8_t optionId;
        uint8_t optionLength;
        while (true) {
            memcpy(&optionId, data++, sizeof(uint8_t));
            if (optionId == OPTION_END) break;
            memcpy(&optionLength, data++, sizeof(uint8_t));
            std::vector<uint8_t> optionBody {};
            uint8_t currentByte;
            for (int i = 0; i < optionLength; i++) {
                memcpy(&currentByte, data++, sizeof(uint8_t));
                optionBody.push_back(currentByte);
            }
            packet.options.emplace(optionId, optionBody);
        }
    }
    return packet;
}

