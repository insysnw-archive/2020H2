//
// Created by Roman Svechnikov on 07.12.2020.
//

#include <csignal>
#include "../common/UdpSocket.h"
#include "protocol/Dns.h"
#include "protocol/QueryMessage.h"
#include "protocol/ResponseMessage.h"

bool shouldExit = false;

void signalHandler(int signum) {
    std::printf("Stopping the server.\n");
    shouldExit = true;
    exit(0);
}

ResponseMessage handleQuery(const QueryMessage &queryMessage);

ResponseMessage generateNotImplementedAnswer(const QueryMessage &queryMessage);

int main() {
    UdpSocket udpSocket{};
    udpSocket.bind(DNS_PORT);

    signal(SIGINT, signalHandler);

    while (!shouldExit) {
        struct sockaddr_in clientAddress{};
        auto[d, l] = udpSocket.recv(clientAddress);
        clientAddress.sin_port = bswap_16(clientAddress.sin_port);
        QueryMessage query{d, l};

        std::printf("ID: %d, QR: %hhu, RD: %d, rcode: %hhu, QDCOUNT: %d.\n",
                    query.getId(), query.getMessageType(),
                    query.isRecursionDesired(), query.getResponseCode(), query.getQuestionEntriesCount());

        std::printf("QUERY: %s, TYPE: %hu, CLASS: %hu\n",
                    query.getQueries()[0].name.c_str(), static_cast<uint16_t>(query.getQueries()[0].type),
                    static_cast<uint16_t>(query.getQueries()[0].qClass));

        try {
            auto response = handleQuery(query);
            udpSocket.sendPacket(response, clientAddress.sin_addr.s_addr, clientAddress.sin_port);
        } catch (const std::exception &exception) {
            auto response = generateNotImplementedAnswer(query);
            udpSocket.sendPacket(response, clientAddress.sin_addr.s_addr, clientAddress.sin_port);
        }
    }
    return 0;
}

ResponseMessage handleQuery(const QueryMessage &queryMessage) {
    ResponseMessage response{queryMessage.getId()};
    response.setResponseCode(Rcode::NO_ERROR);
    response.setRecursionDesired(queryMessage.isRecursionDesired());
    response.setRecursionAvailable(true);
    for (const auto &query :queryMessage.getQueries()) {
        switch (query.type) {
            case QType::A: {
                std::vector<uint8_t> answer{192, 168, 1, 4};
                response.addResourceRecord(ResourceRecord{queryMessage.getQueries()[0].name, query.type, query.qClass, 0, answer});
                break;
            }
            default:
                throw std::exception();
        }
    }
    return response;
}

ResponseMessage generateNotImplementedAnswer(const QueryMessage &queryMessage) {
    ResponseMessage response{queryMessage.getId()};
    response.setResponseCode(Rcode::NOT_IMPLEMENTED);
    response.setRecursionDesired(queryMessage.isRecursionDesired());
    response.setRecursionAvailable(false);
    return response;
}
