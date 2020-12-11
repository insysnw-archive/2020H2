//
// Created by Roman Svechnikov on 11.12.2020.
//

#ifndef NETLAB2_DNS_H
#define NETLAB2_DNS_H

#include <cstdint>


static constexpr uint8_t QR_QUERY = 0;
static constexpr uint8_t QR_RESPONSE = 1;

static constexpr uint8_t OPCODE_QUERY = 0;

static constexpr uint8_t RCODE_NO_ERROR = 0;
static constexpr uint8_t RCODE_NOT_IMPLEMENTED = 4;

static constexpr uint8_t QTYPE_A = 1;
static constexpr uint8_t QTYPE_NS = 2;
static constexpr uint8_t QTYPE_MX = 15;

static constexpr uint8_t QCLASS_IN = 1;

#endif //NETLAB2_DNS_H
