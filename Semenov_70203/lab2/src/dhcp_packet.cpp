#include "dhcp/dhcp_packet.h"

#include <algorithm>
#include <chrono>
#include <iostream>
#include <sstream>

#include "dhcp/log.h"
#include "dhcp/net_int.h"

namespace dhcp {

constexpr uint8_t SUBNET_MASK_CODE = 1;
constexpr uint8_t T1_CODE = 58;
constexpr uint8_t T2_CODE = 59;
constexpr uint8_t BROADCAST_CODE = 28;
constexpr uint8_t ROUTER_CODE = 3;
constexpr uint8_t MESSAGE_TYPE_CODE = 53;
constexpr uint8_t CLIENT_ID_CODE = 61;
constexpr uint8_t REQUESTED_IP_CODE = 50;
constexpr uint8_t LEASE_TIME_CODE = 51;
constexpr uint8_t SERVER_ID_CODE = 54;
constexpr uint8_t DNS_CODE = 6;

RawType DhcpPacket::serialize() const noexcept {
    RawType raw;
    raw += op.toRaw();
    raw += htype.toRaw();
    raw += hlen.toRaw();
    raw += hops.toRaw();
    raw += xid.toRaw();
    raw += secs.toRaw();
    raw += flags.toRaw();
    raw += ciaddr.toRaw();
    raw += yiaddr.toRaw();
    raw += siaddr.toRaw();
    raw += giaddr.toRaw();
    raw += chaddr;
    raw += sname;
    raw += file;
    raw.push_back(99u);
    raw.push_back(130u);
    raw.push_back(83u);
    raw.push_back(99u);

    for (auto & option : mOptions) {
        auto optionSize = static_cast<uint8_t>(option.data.size());
        raw.push_back(option.code);
        raw.push_back(optionSize);
        raw.append(option.data);
    }
    return raw;
}

DhcpPacket::OptionsList parseOptions(
    RawType::const_iterator begin,
    RawType::const_iterator end) noexcept {
    // skip magic numbers
    auto cursor = begin + 4;
    DhcpPacket::OptionsList options;

    while (cursor < end) {
        Option option;
        option.code = *cursor;

        if (option.code == 0 || option.code == 255) {
            cursor += 1;
            continue;
        }

        auto size = *(cursor + 1);
        option.data = RawType{cursor + 2, cursor + 2 + size};
        cursor += size + 2;

        options.emplace_back(std::move(option));
    }

    return options;
}

DhcpPacket DhcpPacket::deserialize(const RawType & raw) noexcept {
    DhcpPacket packet;
    auto rawBegin = raw.begin();

    packet.op.assign(rawBegin);
    rawBegin += packet.op.size;

    packet.htype.assign(rawBegin);
    rawBegin += packet.htype.size;

    packet.hlen.assign(rawBegin);
    rawBegin += packet.hlen.size;

    packet.hops.assign(rawBegin);
    rawBegin += packet.hops.size;

    packet.xid.assign(rawBegin);
    rawBegin += packet.xid.size;

    packet.secs.assign(rawBegin);
    rawBegin += packet.secs.size;

    packet.flags.assign(rawBegin);
    rawBegin += packet.flags.size;

    packet.ciaddr.assign(rawBegin);
    rawBegin += packet.ciaddr.size;

    packet.yiaddr.assign(rawBegin);
    rawBegin += packet.yiaddr.size;

    packet.siaddr.assign(rawBegin);
    rawBegin += packet.siaddr.size;

    packet.giaddr.assign(rawBegin);
    rawBegin += packet.giaddr.size;

    packet.chaddr.append(rawBegin, rawBegin + 16);
    rawBegin += packet.chaddr.size();

    packet.sname.append(rawBegin, rawBegin + 64);
    rawBegin += packet.sname.size();

    packet.file.append(rawBegin, rawBegin + 128);
    rawBegin += packet.file.size();

    packet.mOptions = parseOptions(rawBegin, raw.end());
    return packet;
}

std::string ipV4pack(RawType raw) noexcept {
    std::stringstream ss;
    for (auto i = raw.begin(); i < raw.end() - 4; i += 4) {
        ss << IpType::fromRaw(i).toString() << " ";
    }
    ss << IpType::fromRaw(raw.end() - 4).toString();
    return ss.str();
}

template <class Type>
std::string intPack(RawType raw) noexcept {
    auto size = sizeof(Type);
    std::stringstream ss;
    for (auto i = raw.begin(); i < raw.end() - size; i += size) {
        ss << +NetInt<Type>::fromRaw(i) << " ";
    }
    ss << +NetInt<Type>::fromRaw(raw.end() - size);
    return ss.str();
}

std::string hexValue(RawType raw) noexcept {
    if (raw.size() == 0)
        return "";

    std::stringstream ss;
    for (size_t i = 0; i < raw.size() - 1; i++) {
        ss.fill('0');
        ss.width(2);
        ss << std::hex << +static_cast<uint8_t>(raw[i]) << ":";
    }

    ss.fill('0');
    ss.width(2);
    ss << std::hex << +static_cast<uint8_t>(raw.back());

    return ss.str();
}

#define OPT_BASE(code, description, str)              \
    case (code):                                      \
        ss << (description) << ": " << (str) << "\n"; \
        break;

#define OPT_IP(code, description) \
    OPT_BASE((code), (description), ipV4pack(o.data))

#define OPT_UI(code, description) \
    OPT_BASE((code), (description), intPack<uint32_t>(o.data))

#define OPT_US(code, description) \
    OPT_BASE((code), (description), intPack<uint16_t>(o.data))

#define OPT_FLAG(code, description) \
    OPT_BASE((code), (description), intPack<bool>(o.data));

#define OPT_BYTE(code, description) \
    OPT_BASE((code), (description), intPack<uint8_t>(o.data));

#define OPT_STR(code, description) OPT_BASE((code), (description), o.data);

#define OPT_HEX(code, description) \
    OPT_BASE((code), (description), hexValue(o.data));

std::string optionsString(const DhcpPacket::OptionsList & options) noexcept {
    std::stringstream ss;
    for (auto & o : options) {
        ss << "option (" << +o.code << ") -- ";
        switch (o.code) {
            OPT_IP(SUBNET_MASK_CODE, "Subnet mask");
            OPT_UI(2, "Time offset");
            OPT_IP(ROUTER_CODE, "Router");
            OPT_IP(4, "Time server");
            OPT_IP(5, "Name server");
            OPT_IP(DNS_CODE, "Domain name server");
            OPT_IP(7, "Log server");
            OPT_IP(8, "Cookie server");
            OPT_IP(9, "LPR server");
            OPT_IP(10, "Impress server");
            OPT_IP(11, "Resource location server");
            OPT_STR(12, "Host name");
            OPT_US(13, "Boot file size");
            OPT_STR(14, "Merit dump file");
            OPT_STR(15, "Domain name");
            OPT_IP(16, "Swap server");
            OPT_STR(17, "Root path");
            OPT_STR(18, "Extensions path");
            OPT_FLAG(19, "IP forwarding");
            OPT_FLAG(20, "Non-local source routing");
            OPT_IP(21, "Policy filter");
            OPT_US(22, "Maximum datagram reassembly size");
            OPT_BYTE(23, "Default IP time-to-live");
            OPT_UI(24, "Path MTU aging timeout");
            OPT_US(25, "Path MTU table");
            OPT_US(26, "Interface MTU");
            OPT_FLAG(27, "All subnets are local");
            OPT_IP(BROADCAST_CODE, "Broadcast address");
            OPT_FLAG(29, "Perform mask discovery");
            OPT_FLAG(30, "Mask supplier");
            OPT_FLAG(31, "Perform router discovery");
            OPT_IP(32, "Router solication address");
            OPT_IP(33, "Static route option");
            OPT_FLAG(34, "Trailer encapsulation");
            OPT_UI(35, "ARP cache timeout");
            OPT_FLAG(36, "Ethernet encapsulation");
            OPT_BYTE(37, "TCP default TTL");
            OPT_UI(38, "TCP keepalive interval");
            OPT_FLAG(39, "TCP keepalive garbage");
            OPT_STR(40, "Network information service domain");
            OPT_IP(41, "Network information servers");
            OPT_IP(42, "Network time protocol servers");
            // skip vendor option
            OPT_IP(44, "NetBIOS over TCP/IP name server");
            OPT_IP(45, "NetBIOS over TCP/IP datagram distribution server");
            OPT_BYTE(46, "NetBIOS over TCP/IP node type");
            OPT_STR(47, "NetBIOS over TCP/IP scope");
            OPT_IP(48, "X window system font server");
            OPT_IP(49, "X window system display manager");
            OPT_IP(REQUESTED_IP_CODE, "Requested ip address");
            OPT_UI(LEASE_TIME_CODE, "IP address lease time");
            OPT_BYTE(52, "Overload");
            OPT_IP(SERVER_ID_CODE, "Server identifier");
            OPT_BYTE(55, "Parameter request");
            OPT_STR(56, "Message");
            OPT_US(57, "Maximum DHCP message size");
            OPT_UI(58, "Renewal (T1) time value");
            OPT_UI(59, "Rebinding (T2) time value");
            OPT_STR(60, "Class-identifier");
            OPT_HEX(CLIENT_ID_CODE, "Client-identifier");

            case MESSAGE_TYPE_CODE:
                ss << "DHCP Message type: ";
                switch (net8::fromRaw(o.data)) {
                    case 1: ss << "DHCPDISCOVER\n"; break;
                    case 2: ss << "DHCPOFFER\n"; break;
                    case 3: ss << "DHCPREQUEST\n"; break;
                    case 4: ss << "DHCPDECLINE\n"; break;
                    case 5: ss << "DHCPACK\n"; break;
                    case 6: ss << "DHCPNACK\n"; break;
                    case 7: ss << "DHCPRELEASE\n"; break;
                    case 8: ss << "DHCPINFORM\n"; break;
                    default: ss << "Error\n"; break;
                }
                break;
            default: ss << "Unkown option\n"; break;
        }
    }
    return ss.str();
}

void DhcpPacket::print() const noexcept {
    auto now = std::chrono::system_clock::now();
    auto time = std::chrono::system_clock::to_time_t(now);

    std::cout << "======================================================="
              << "\n";
    std::cout << "\t\t" << ctime(&time);
    std::cout << "======================================================="
              << "\n";
    std::cout << "    op: " << +op << "\n";
    std::cout << " htype: " << +htype << "\n";
    std::cout << "  hlen: " << +hlen << "\n";
    std::cout << "  hops: " << +hops << "\n";
    std::cout << "   xid: " << std::hex << xid << "\n";
    std::cout << "  secs: " << secs << "\n";
    std::cout << " flags: " << flags << "\n";

    std::cout << "ciaddr: " << ciaddr.toString() << "\n";
    std::cout << "yiaddr: " << yiaddr.toString() << "\n";
    std::cout << "siaddr: " << siaddr.toString() << "\n";
    std::cout << "giaddr: " << giaddr.toString() << "\n";

    std::cout << "chaddr: " << hexValue(chaddr) << "\n";
    std::cout << " sname: " << sname << "\n";
    std::cout << "  file: " << file << "\n\n";

    std::cout << optionsString(mOptions) << std::endl;
}

// Options

template <class Container>
auto findOption(Container && container, uint8_t code) {
    auto iter = std::find_if(
        container.begin(), container.end(),
        [code](const Option & option) { return option.code == code; });
    return iter;
}

Option * DhcpPacket::getOption(uint8_t code) noexcept {
    auto iter = findOption(mOptions, code);
    if (iter == mOptions.end())
        return nullptr;
    return iter.base();
}

std::optional<Option> DhcpPacket::getOption(uint8_t code) const noexcept {
    auto iter = findOption(mOptions, code);
    if (iter == mOptions.end())
        return std::nullopt;
    return *iter;
}

void DhcpPacket::addOption(Option && option) noexcept {
    auto createdOption = getOption(option.code);
    if (createdOption)
        createdOption->data = std::move(option.data);
    else
        mOptions.emplace_back(std::move(option));
}

void DhcpPacket::clearOptions() noexcept {
    mOptions.clear();
}

RawType DhcpPacket::clientId() const noexcept {
    auto cid = chaddr;
    auto clientId = getOption(CLIENT_ID_CODE);
    if (clientId.has_value())
        cid.append(clientId->data);

    return cid;
}

std::optional<IpType> DhcpPacket::requestedIp() const noexcept {
    auto requestedIp = getOption(REQUESTED_IP_CODE);
    if (!requestedIp.has_value())
        return UNDEFINED_IP;
    return IpType::fromRaw(requestedIp->data);
}

MessageType DhcpPacket::messageType() const noexcept {
    auto messageType = getOption(MESSAGE_TYPE_CODE);
    if (!messageType.has_value())
        return MessageType::UNDEFINED;
    uint8_t type = net8::fromRaw(messageType->data);
    return static_cast<MessageType>(type);
}

void DhcpPacket::setMessageType(MessageType type) noexcept {
    uint8_t value = static_cast<uint8_t>(type);
    Option messageType;
    messageType.code = MESSAGE_TYPE_CODE;
    messageType.data = net8{value}.toRaw();
    addOption(std::move(messageType));
}

#define ADD_OPTION(CODE, VALUE)    \
    Option option;                 \
    option.code = (CODE);          \
    option.data = (VALUE).toRaw(); \
    addOption(std::move(option));

void DhcpPacket::setServerId(IpType ip) noexcept {
    ADD_OPTION(SERVER_ID_CODE, ip);
}

std::optional<IpType> DhcpPacket::getServerId() const noexcept {
    auto serverId = getOption(SERVER_ID_CODE);
    if (serverId.has_value())
        return IpType::fromRaw(serverId->data);
    return UNDEFINED_IP;
}

void DhcpPacket::setSubnetMask(IpType ip) noexcept {
    ADD_OPTION(SUBNET_MASK_CODE, ip);
}

void DhcpPacket::setDnsServer(IpType ip) noexcept {
    ADD_OPTION(DNS_CODE, ip);
}

void DhcpPacket::setRouter(IpType ip) noexcept {
    ADD_OPTION(ROUTER_CODE, ip);
}

void DhcpPacket::setBroadcast(IpType ip) noexcept {
    ADD_OPTION(BROADCAST_CODE, ip);
}

std::optional<net32> DhcpPacket::getLeaseTime() const noexcept {
    auto leaseTime = getOption(LEASE_TIME_CODE);
    if (leaseTime.has_value())
        return net32::fromRaw(leaseTime->data);
    return std::nullopt;
}

void DhcpPacket::setLeaseTime(net32 time) noexcept {
    ADD_OPTION(LEASE_TIME_CODE, time);
}

void DhcpPacket::setT1(net32 time) noexcept {
    ADD_OPTION(T1_CODE, time);
}

void DhcpPacket::setT2(net32 time) noexcept {
    ADD_OPTION(T2_CODE, time);
}

IpType DhcpPacket::ipAddress() const noexcept {
    if (giaddr != 0)
        return giaddr;

    auto broadcast = IpType::fromNet(INADDR_BROADCAST);
    if (messageType() == MessageType::DHCPNACK)
        return broadcast;

    if (ciaddr != 0)
        return ciaddr;

    if (flags & (1 << 31))
        return broadcast;

    return broadcast;
}

}  // namespace dhcp
