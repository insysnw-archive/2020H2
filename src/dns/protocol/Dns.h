//
// Created by Roman Svechnikov on 11.12.2020.
//

#ifndef NETLAB2_DNS_H
#define NETLAB2_DNS_H

#include <cstdint>
#include <string>
#include <vector>

static constexpr uint8_t DNS_PORT = 53;

enum class MessageType: uint8_t {
    QUERY = 0,
    RESPONSE = 1
};

enum class Opcode: uint8_t {
    QUERY = 0,
    IQUERY = 1,
    STATUS = 2
};

enum class Rcode: uint8_t {
    NO_ERROR = 0,
    FORMAT_ERROR = 1,
    SERVER_FAILURE = 2,
    NAME_ERROR = 3,
    NOT_IMPLEMENTED = 4,
    REFUSED = 5
};

enum class QType: uint16_t {
    A = 1,
    NS = 2,
    MD = 3,
    MF = 4,
    CNAME = 5,
    SOA = 6,
    WKS = 11,
    PTR = 12,
    HINFO = 13,
    MINFO = 14,
    MX = 15,
    TXT = 16,
    AAAA = 28,
    AXFR = 252,
    MAILB = 253,
    MAILA = 254,
    ALL = 255
};

enum class QClass: uint16_t {
    IN = 1,
    CS = 2,
    CH = 3,
    HS = 4
};

struct DnsHeader {
    uint16_t id;
    uint16_t flags; // QR, Opcode, AA, TC, RD, RA, RCODE
    uint16_t qdCount;
    uint16_t anCount;
    uint16_t nsCount;
    uint16_t arCount;
};

struct Query {
    std::string name;
    QType type;
    QClass qClass;
};

struct ResourceRecord {
    std::string name;
    QType type;
    QClass qClass;
    uint32_t ttl;
    std::vector<uint8_t> recordData;
};

#endif //NETLAB2_DNS_H
