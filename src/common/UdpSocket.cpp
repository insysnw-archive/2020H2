//
// Created by Roman Svechnikov on 08.12.2020.
//

#include "UdpSocket.h"

UdpSocket::UdpSocket() {
    WSADATA wsaData;
    if (WSAStartup(0x0202, &wsaData) != 0) {
        std::fprintf(stderr, "ERROR! Where: WSAStartup\n");
        exit(1);
    }
    sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
}

UdpSocket::~UdpSocket() {
    WSACleanup();
    closesocket(sock);
}

void UdpSocket::recv() {
    auto l = recvfrom(sock, buffer, 512, 0, nullptr, nullptr);
    if (l > 0) {
        buffer[l] = '\0';
        std::printf("Result: %s\n", buffer);
    } else if (l == 0) {
        std::printf("No input\n");
    } else {
        std::fprintf(stderr, "recv failed\n");
    }

}

void UdpSocket::bind(uint16_t port) {
    sockaddr_in address{};
    address.sin_family = AF_INET;
    address.sin_addr.S_un.S_addr = INADDR_ANY;
    address.sin_port = htons(port);
    auto status = ::bind(sock, (struct sockaddr *) &address, sizeof(address));
    if (status < 0) {
        perror("bind");
        exit(1);
    }
}

