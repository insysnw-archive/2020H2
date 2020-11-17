#include "dhcp/dhcp_package.h"

#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/types.h>
#include <iostream>
#include <iterator>
#include <sstream>

#include "dhcp/net_int.h"

namespace dhcp {

RawType DhcpPackage::serialize() const noexcept {
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

    for (auto & option : options) {
        auto optionSize = static_cast<uint8_t>(option.data.size());
        raw.push_back(option.code);
        raw.push_back(optionSize);
        raw.append(option.data);
    }

    return raw;
}

DhcpPackage::OptionsList parseOptions(
    RawType::const_iterator begin,
    RawType::const_iterator end) noexcept {
    // skip magic numbers
    auto cursor = begin + 4;
    DhcpPackage::OptionsList options;

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

DhcpPackage DhcpPackage::deserialize(const RawType & raw) noexcept {
    DhcpPackage package;
    auto rawBegin = raw.begin();

    package.op.fromRaw(rawBegin);
    rawBegin += package.op.size;

    package.htype.fromRaw(rawBegin);
    rawBegin += package.htype.size;

    package.hlen.fromRaw(rawBegin);
    rawBegin += package.hlen.size;

    package.hops.fromRaw(rawBegin);
    rawBegin += package.hops.size;

    package.xid.fromRaw(rawBegin);
    rawBegin += package.xid.size;

    package.secs.fromRaw(rawBegin);
    rawBegin += package.secs.size;

    package.flags.fromRaw(rawBegin);
    rawBegin += package.flags.size;

    package.ciaddr.fromRaw(rawBegin);
    rawBegin += package.ciaddr.size;

    package.yiaddr.fromRaw(rawBegin);
    rawBegin += package.yiaddr.size;

    package.siaddr.fromRaw(rawBegin);
    rawBegin += package.siaddr.size;

    package.giaddr.fromRaw(rawBegin);
    rawBegin += package.giaddr.size;

    package.chaddr.append(rawBegin, rawBegin + 16);
    rawBegin += package.chaddr.size();

    package.sname.append(rawBegin, rawBegin + 64);
    rawBegin += package.sname.size();

    package.file.append(rawBegin, rawBegin + 128);
    rawBegin += package.file.size();

    package.options = parseOptions(rawBegin, raw.end());
    return package;
}

std::string ipV4(IpType ip) noexcept {
    char buffer[INET_ADDRSTRLEN];
    in_addr inaddr;
    inaddr.s_addr = ip.net();

    if (!inet_ntop(AF_INET, &inaddr, buffer, sizeof(buffer)))
        return "Error";
    return std::string{buffer};
}

std::string ipV4pack(RawType raw) noexcept {
    std::stringstream ss;
    for (auto i = raw.begin(); i < raw.end() - 4; i += 4) {
        ss << ipV4(IpType{i}) << " ";
    }
    ss << ipV4(IpType{raw.end() - 4});
    return ss.str();
}

template <class Type>
std::string intPack(RawType raw) noexcept {
    auto size = sizeof(Type);
    std::stringstream ss;
    for (auto i = raw.begin(); i < raw.end() - size; i += size) {
        ss << +NetInt<Type>{i} << " ";
    }
    ss << +NetInt<Type>{raw.end() - size};
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

std::string optionsString(const DhcpPackage::OptionsList & options) noexcept {
    std::stringstream ss;
    for (auto & o : options) {
        ss << "option (" << +o.code << ") -- ";
        switch (o.code) {
            OPT_IP(1, "Subnet mask");
            OPT_UI(2, "Time offset");
            OPT_IP(3, "Router");
            OPT_IP(4, "Time server");
            OPT_IP(5, "Name server");
            OPT_IP(6, "Domain name server");
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
            OPT_IP(28, "Broadcast address");
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
            OPT_IP(50, "Requested ip address");
            OPT_UI(51, "IP address lease time");
            OPT_BYTE(52, "Overload");
            OPT_IP(54, "Server identifier");
            OPT_BYTE(55, "Parameter request");
            OPT_STR(56, "Message");
            OPT_US(57, "Maximum DHCP message size");
            OPT_UI(58, "Renewal (T1) time value");
            OPT_UI(59, "Rebinding (T2) time value");
            OPT_STR(60, "Class-identifier");
            OPT_HEX(61, "Client-identifier");

            case 53:
                ss << "DHCP Message type: ";
                switch (NetInt<uint8_t>{o.data}) {
                    case 1: ss << "DHCPDISCOVER\n"; break;
                    case 2: ss << "DHCPOFFER\n"; break;
                    case 3: ss << "DHCPREQUEST\n"; break;
                    case 4: ss << "DHCPDECLINE\n"; break;
                    case 5: ss << "DHCPACK\n"; break;
                    case 6: ss << "DHCPNACK\n"; break;
                    case 7: ss << "DHCPRELEASE\n"; break;
                    default: ss << "Error" << std::endl;
                }
                break;
            default: ss << "Unkown option"; break;
        }
    }
    return ss.str();
}

void DhcpPackage::print() const noexcept {
    std::cout << "    op: " << +op << "\n";
    std::cout << " htype: " << +htype << "\n";
    std::cout << "  hlen: " << +hlen << "\n";
    std::cout << "  hops: " << +hops << "\n";
    std::cout << "   xid: " << std::hex << xid << "\n";
    std::cout << "  secs: " << secs << "\n";
    std::cout << " flags: " << flags << "\n";

    std::cout << "ciaddr: " << ipV4(ciaddr) << "\n";
    std::cout << "yiaddr: " << ipV4(yiaddr) << "\n";
    std::cout << "siaddr: " << ipV4(siaddr) << "\n";
    std::cout << "giaddr: " << ipV4(giaddr) << "\n";

    std::cout << "chaddr: " << hexValue(chaddr) << "\n";
    std::cout << " sname: " << sname << "\n";
    std::cout << "  file: " << file << "\n\n";

    std::cout << "Options\n" << optionsString(options) << std::endl;
}

}  // namespace dhcp
