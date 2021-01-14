//
// Created by Roman Svechnikov on 07.12.2020.
//

#include "../common/UdpSocket.h"
#include "protocol/Dns.h"
#include "protocol/Message.h"

int main() {
    UdpSocket udpSocket{};
    udpSocket.bind(DNS_PORT);

    auto[d, l] = udpSocket.recv();
    Message message{d, l};

    std::printf("ID: %d, QR: %hhu, RD: %d, rcode: %hhu, QDCOUNT: %d.\n",
                message.getId(), message.getMessageType(),
                message.isRecursionDesired(), message.getResponseCode(), message.getQuestionEntriesCount());

    std::printf("QUERY: %s, TYPE: %hu, CLASS: %hu\n",
                message.getQueries()[0].name.c_str(), static_cast<uint16_t>(message.getQueries()[0].type),
                static_cast<uint16_t>(message.getQueries()[0].qClass));

    return 0;
}