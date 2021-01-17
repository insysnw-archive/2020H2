//
// Created by Roman Svechnikov on 07.12.2020.
//

#ifndef NETLAB2_DHCP_H
#define NETLAB2_DHCP_H

#include <cstdlib>
#include <cstring>
#include <cstdint>
#include <utility>
#include <vector>
#include <unordered_map>

using options_t = std::unordered_map<uint8_t, std::vector<uint8_t>>;

struct DhcpBase {
    uint8_t op {};
    uint8_t htype {};
    uint8_t hlen {};
    uint8_t hops {};
    uint32_t xid {};
    uint16_t secs {};
    uint16_t flags {};
    uint32_t ciaddr {};
    uint32_t yiaddr {};
    uint32_t siaddr {};
    uint32_t giaddr {};
    uint8_t chaddr[16] {};
    char sname[64] {};
    char file[128] {};
};

struct Dhcp {
    Dhcp();
    DhcpBase *dhcpBase;
    options_t options;

    DhcpBase *operator->() const {
        return dhcpBase;
    }

    [[nodiscard]] std::pair<char *, size_t> serialize() const;
};

static constexpr uint8_t DHCP_BOOTREQUEST = 0x01;
static constexpr uint8_t DHCP_BOOTREPLY = 0x02;

static constexpr uint8_t DHCP_SERVER_PORT = 67;
static constexpr uint8_t DHCP_CLIENT_PORT = 68;

static constexpr uint8_t HARDWARE_TYPE_ETHERNET_10 = 1;

static constexpr uint8_t OPTION_REQUESTED_IP = 50;
static constexpr uint8_t OPTION_IP_LEASE_TIME = 51;
static constexpr uint8_t OPTION_MESSAGE_TYPE = 53;
static constexpr uint8_t OPTION_SERVER_IDENTIFIER = 54;
static constexpr uint8_t OPTION_END = 255;

static constexpr uint8_t MESSAGE_TYPE_DISCOVER = 1;
static constexpr uint8_t MESSAGE_TYPE_OFFER = 2;
static constexpr uint8_t MESSAGE_TYPE_REQUEST = 3;
static constexpr uint8_t MESSAGE_TYPE_DECLINE = 4;
static constexpr uint8_t MESSAGE_TYPE_ACK = 5;

static constexpr uint32_t DHCP_MAGIC_COOKIE = 0x63538263;
#endif //NETLAB2_DHCP_H
