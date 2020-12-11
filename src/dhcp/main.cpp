//
// Created by Roman Svechnikov on 07.12.2020.
//

#include <cstring>
#include "protocol/Dhcp.h"
#include "../common/UdpSocket.h"
#include "util.h"

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
    packet->flags = 0;
    packet->ciaddr = 0;
    packet->yiaddr = 0;
    packet->siaddr = 0;
    packet->giaddr = 0;
    memcpy(packet->chaddr, mac.data(), mac.size());
    uint8_t options[]{
            OPTION_MESSAGE_TYPE, 1, MESSAGE_TYPE_REQUEST,
            OPTION_REQUESTED_IP, 4, selectedIp[3], selectedIp[2], selectedIp[1], selectedIp[0],
            OPTION_SERVER_IDENTIFIER, 4, selectedServer[3], selectedServer[2], selectedServer[1], selectedServer[0],
            OPTION_END
    };

    packet.options = options;
    packet.optionsSize = sizeof(options);

    return packet;
}

Dhcp buildDhcpDiscoveryPacket(uint32_t xid, std::array<uint8_t, 6> mac) {
    Dhcp packet{};

    packet->op = DHCP_BOOTREQUEST;
    packet->htype = HARDWARE_TYPE_ETHERNET_10;
    packet->hlen = mac.size();
    packet->hops = 0;
    packet->xid = xid;
    packet->secs = 0;
    packet->flags = 0;
    packet->ciaddr = 0;
    packet->yiaddr = 0;
    packet->siaddr = 0;
    packet->giaddr = 0;
    memcpy(packet->chaddr, mac.data(), mac.size());

    uint8_t options[]{
            OPTION_MESSAGE_TYPE, 1, MESSAGE_TYPE_DISCOVER,
            OPTION_END
    };
    packet.options = options;
    packet.optionsSize = sizeof(options);

    return packet;
}

int main(int argc, char *argv[]) {

    if (argc < 2 || (std::strcmp(argv[1], "-h") == 0)) {
        std::printf("Usage: %s <mac>", argv[0]);
        return 0;
    }
    auto macArg = std::string(argv[1]);
    auto mac = util::parseMac(macArg);

    UdpSocket udpSocket{};
    udpSocket.bind(DHCP_CLIENT_PORT);

    // DHCP DISCOVERY
    uint32_t xid = std::rand() % UINT32_MAX;
    auto discoveryPacket = buildDhcpDiscoveryPacket(xid, mac);
    std::string subnet = "192.168.255.255";  // 255.255.255.255 ??
    udpSocket.sendPacket(discoveryPacket, subnet, DHCP_SERVER_PORT);

    // Loop waiting for DHCP OFFER
//    udpSocket.recv();


    return 0;
}