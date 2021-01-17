//
// Created by Roman Svechnikov on 08.12.2020.
//

#ifndef NETLAB2_UDPSOCKET_H
#define NETLAB2_UDPSOCKET_H

#ifdef _WIN32

#include <winsock2.h>
#include <ws2tcpip.h>

#else

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include <unistd.h>

#endif

#include <string>

struct Packet {
    const char *data;
    long length;
};

class UdpSocket {

#ifdef _WIN32
    using socket_t = SOCKET;
#else
    using socket_t = int;
#endif
public:
    UdpSocket();

    virtual ~UdpSocket();

    void bind(in_addr_t ip, uint16_t port);

    template<typename Packet>
    void sendPacket(Packet packet, in_addr_t ip, uint16_t port) {
        sockaddr_in address{};
        address.sin_family = AF_INET;
#ifdef _WIN32
        address.sin_addr.S_un.S_addr = inet_addr(ip.c_str());
#else
        address.sin_addr.s_addr = ip;
#endif
        address.sin_port = htons(port);

        auto[d, l] = packet.serialize();
        sendto(sock, d, l, 0, (sockaddr *) &address, sizeof(address));
        free(d);
    }

    template<typename Packet>
    void sendPacket(Packet packet, const std::string &ip, uint16_t port) {
        sendPacket(packet, inet_addr(ip.c_str()), port);
    }

    Packet recv(sockaddr_in &clientAddress);

    void enableBroadcast();

private:
    socket_t sock;
    char buffer[512];
};


#endif //NETLAB2_UDPSOCKET_H
