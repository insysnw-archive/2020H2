//
// Created by Roman Svechnikov on 07.12.2020.
//

#include <cstring>
#include "protocol/DhcpPackets.h"
#include "../common/UdpSocket.h"
#include "util.h"

int main(int argc, char *argv[]) {

    if (argc < 2 || (std::strcmp(argv[1], "-h") == 0)) {
        std::printf("Usage: %s <mac>\n", argv[0]);
        return 0;
    }
    auto macArg = std::string(argv[1]);
    auto mac = util::parseMac(macArg);

    UdpSocket udpSocket{};
    udpSocket.enableBroadcast();
    udpSocket.bind(INADDR_ANY, DHCP_CLIENT_PORT);

    // Session ID
    uint32_t xid = std::rand() % UINT32_MAX;

    // DHCP DISCOVERY
    auto discoveryPacket = buildDhcpDiscoveryPacket(xid, mac);
    udpSocket.sendPacket(discoveryPacket, INADDR_BROADCAST, DHCP_SERVER_PORT);
    std::printf("Sending DHCP Discovery packet...\n\n");

    // Wait for DHCP OFFER
    struct sockaddr_in otherAddress{};
    auto[d, l] = udpSocket.recv(otherAddress);
    Dhcp offer = readDhcpPacket(d, l);

    if (!offer.options.empty()) {
        uint8_t packetType = offer.options.at(OPTION_MESSAGE_TYPE)[0];
        if (packetType == MESSAGE_TYPE_OFFER) {
            std::vector<uint8_t> serverId = offer.options.at(OPTION_SERVER_IDENTIFIER);
            std::vector<uint8_t> leaseTimeVec = offer.options.at(OPTION_IP_LEASE_TIME);
            uint32_t leaseTime = ((uint32_t)leaseTimeVec[0] << 24u) + ((uint32_t)leaseTimeVec[1] << 16u) + ((uint32_t)leaseTimeVec[2] << 8u) + (uint32_t)leaseTimeVec[3];
            std::printf("An offer was received from %d.%d.%d.%d.\n", serverId[0], serverId[1], serverId[2], serverId[3]);
            std::printf("Offered address: %s. Lease time is: %d s\n\n", inet_ntoa({offer->yiaddr}), leaseTime);

            // DHCP REQUEST
            std::printf("Requesting offered address...\n\n");
            std::array<uint8_t, 4> requestedIp {
                    (uint8_t)((offer->yiaddr & 0xff000000u) >> 24u),
                    (uint8_t)((offer->yiaddr & 0x00ff0000u) >> 16u),
                    (uint8_t)((offer->yiaddr & 0x0000ff00u) >> 8u),
                    (uint8_t)((offer->yiaddr & 0x000000ffu))
            };
            std::array<uint8_t, 4> serverIdent {
                serverId[3], serverId[2], serverId[1], serverId[0]
            };
            auto requestPacket = buildDhcpRequestPacket(xid, mac, requestedIp, serverIdent);
            udpSocket.sendPacket(requestPacket, INADDR_BROADCAST, DHCP_SERVER_PORT);

            // Wait for DHCP Acknowledgement
            auto[d, l] = udpSocket.recv(otherAddress);
            Dhcp acknowledgement = readDhcpPacket(d, l);
            packetType = acknowledgement.options.at(OPTION_MESSAGE_TYPE)[0];
            if (packetType == MESSAGE_TYPE_ACK) {
                std::printf("A request was approved.\n");
            }
        } else {
            std::printf("That's not an offer! :(\n");
        }
    } else {
        std::printf("Options are empty! That's not an offer! :(\n");
    }

    return 0;
}