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
    std::printf("\nStopping the server.\n");
    shouldExit = true;
    exit(0);
}

ResponseMessage handleQuery(const QueryMessage &queryMessage);

ResponseMessage generateNotImplementedAnswer(const QueryMessage &queryMessage);

int main(int argc, char* argv[]) {
    std::string host;
    if (argc == 1) {
        host = "127.0.0.53";
    } else if (argc == 2) {
        host = std::string(argv[1]);
    } else {
        std::printf("Invalid arguments count.\n"
                    "Use no arguments to bind the server to the 127.0.0.53 or pass an IP as an argument.\n");
        exit(0);
    }
    UdpSocket udpSocket{};
    udpSocket.bind(inet_addr(host.c_str()), DNS_PORT);
    std::printf("Starting server on %s.\n", host.c_str());

    signal(SIGINT, signalHandler);

    while (!shouldExit) {
        struct sockaddr_in clientAddress{};
        auto[d, l] = udpSocket.recv(clientAddress);
        clientAddress.sin_port = bswap_16(clientAddress.sin_port);
        QueryMessage query{d, l};

        std::printf("Query message received.\n");
        std::printf("\tID: %d, QR: %hhu, RD: %d, rcode: %hhu, QDCOUNT: %d.\n",
                    query.getId(), query.getMessageType(),
                    query.isRecursionDesired(), query.getResponseCode(), query.getQuestionEntriesCount());

        std::printf("\tQUERY: %s, TYPE: %hu, CLASS: %hu\n",
                    query.getQueries()[0].name.c_str(), static_cast<uint16_t>(query.getQueries()[0].type),
                    static_cast<uint16_t>(query.getQueries()[0].qClass));

        try {
            auto response = handleQuery(query);
            std::printf("This query is supported. A predefined answer has been sent.\n");
            udpSocket.sendPacket(response, clientAddress.sin_addr.s_addr, clientAddress.sin_port);
        } catch (const std::exception &exception) {
            auto response = generateNotImplementedAnswer(query);
            std::printf("This query is not supported. An answer with RCODE = 'Not Implemented' has been sent.\n");
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
                response.addResourceRecord(ResourceRecord{query.name, query.type, query.qClass, 0, answer});
                break;
            }
            case QType::AAAA: {
                std::vector<uint8_t> answer(16, 0);
                answer[12] = 192;
                answer[13] = 168;
                answer[14] = 1;
                answer[15] = 4;
                response.addResourceRecord(ResourceRecord{query.name, query.type, query.qClass, 0, answer});
                break;
            }
            case QType::MX: {
                std::vector<uint8_t> answer1 {
                    0, 1, // Preference
                    4, 'm', 'a', 'i', 'l', 4, 'f', 'a', 'k', 'e', 0 // Exchange
                };
                response.addResourceRecord(ResourceRecord{query.name, query.type, query.qClass, 0, answer1});
                std::vector<uint8_t> answer2 {
                    0, 10, // Preference
                    5, 'm', 'a', 'i', 'l', '1', 4, 'f', 'a', 'k', 'e', 0 // Exchange
                };
                response.addResourceRecord(ResourceRecord{query.name, query.type, query.qClass, 0, answer2});
                break;
            }
            case QType::TXT: {
                std::vector<uint8_t> answer {
                    11, 'H', 'e', 'l', 'l', 'o', ' ', 't', 'h', 'e', 'r', 'e' // Exchange
                };
                response.addResourceRecord(ResourceRecord{query.name, query.type, query.qClass, 0, answer});
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
