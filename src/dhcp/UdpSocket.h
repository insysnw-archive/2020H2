//
// Created by Roman Svechnikov on 08.12.2020.
//

#ifndef NETLAB2_UDPSOCKET_H
#define NETLAB2_UDPSOCKET_H

#include <winsock2.h>
#include <ws2tcpip.h>
#include <string>

class UdpSocket {
public:
    UdpSocket();

    virtual ~UdpSocket();

    void bind(uint16_t port);

    template<typename Packet>
    void sendPacket(Packet packet, const std::string &ip, uint16_t port) {
        sockaddr_in address {};
        address.sin_family=AF_INET;
        address.sin_addr.S_un.S_addr=inet_addr(ip.c_str());
        address.sin_port=htons(port);

        auto [d, l] = packet.serialize();
        sendto(sock, d, l, 0, (sockaddr *) &address, sizeof(address));
        free(d);
    }

    void recv();

private:
    SOCKET sock;
    char buffer[512];
};


#endif //NETLAB2_UDPSOCKET_H
