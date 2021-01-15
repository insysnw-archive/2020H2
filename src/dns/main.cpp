//
// Created by Roman Svechnikov on 07.12.2020.
//

#include "../common/UdpSocket.h"
#include "protocol/Dns.h"
#include "protocol/QueryMessage.h"
#include "protocol/ResponseMessage.h"

int main() {
    UdpSocket udpSocket{};
    udpSocket.bind(DNS_PORT);

    auto[d, l] = udpSocket.recv();
    QueryMessage query{d, l};

    std::printf("ID: %d, QR: %hhu, RD: %d, rcode: %hhu, QDCOUNT: %d.\n",
                query.getId(), query.getMessageType(),
                query.isRecursionDesired(), query.getResponseCode(), query.getQuestionEntriesCount());

    std::printf("QUERY: %s, TYPE: %hu, CLASS: %hu\n",
                query.getQueries()[0].name.c_str(), static_cast<uint16_t>(query.getQueries()[0].type),
                static_cast<uint16_t>(query.getQueries()[0].qClass));

    ResponseMessage response{query.getId()};
    response.addResourceRecord(ResourceRecord{"ya.ru.", QType::A, QClass::IN, 0, "test"});


    udpSocket.sendPacket(response, "127.0.0.2", 123);

    return 0;
}