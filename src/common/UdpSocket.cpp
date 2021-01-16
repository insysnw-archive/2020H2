//
// Created by Roman Svechnikov on 08.12.2020.
//

#include "UdpSocket.h"

UdpSocket::UdpSocket() {
#ifdef _WIN32
    WSADATA wsaData;
    if (WSAStartup(0x0202, &wsaData) != 0) {
        std::fprintf(stderr, "ERROR! Where: WSAStartup\n");
        exit(1);
    }
#endif
    sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
}

UdpSocket::~UdpSocket() {
#ifdef _WIN32
    WSACleanup();
    closesocket(sock);
#else
    close(sock);
#endif
}

Packet UdpSocket::recv(sockaddr_in &clientAddress) {
    socklen_t addrLen = sizeof(struct sockaddr_in);
    auto l = recvfrom(sock, buffer, 512, 0, (struct sockaddr *) &clientAddress, &addrLen);
    if (l > 0) {
        return Packet{buffer, l};
    } else {
        std::fprintf(stderr, "recv failed\n");
        return Packet{nullptr, -1};
    }
}

void UdpSocket::bind(uint16_t port) {
    sockaddr_in address{};
    address.sin_family = AF_INET;
#ifdef _WIN32
    address.sin_addr.S_un.S_addr = INADDR_ANY;
#else
    address.sin_addr.s_addr = inet_addr("127.0.0.53");
#endif
    address.sin_port = htons(port);
    auto status = ::bind(sock, (struct sockaddr *) &address, sizeof(address));
    if (status < 0) {
        perror("bind");
        exit(1);
    }
}

