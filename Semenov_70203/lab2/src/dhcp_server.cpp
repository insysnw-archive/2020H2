#include "dhcp/dhcp_server.h"

#include <bits/stdint-uintn.h>
#include <sys/socket.h>
#include <unistd.h>
#include <array>
#include <cstring>
#include <limits>
#include <optional>

#include "dhcp/common.h"
#include "dhcp/config.h"
#include "dhcp/dhcp_packet.h"
#include "dhcp/leased_ip.h"

namespace dhcp {

DhcpServer::DhcpServer(const Config & config) noexcept
    : mStopped{false}, mConfig{config}, mAllocator{config.range} {
    mSocket = bindedSocket(config);
    mId = stringToIp(config.address);

    auto broadcastConfig = config;
    broadcastConfig.address = "255.255.255.255";
    mBroadcastSocket = bindedSocket(broadcastConfig);

    logInfo(
        "Ip from " + ipToString(config.range.from()) + " to " +
        ipToString(config.range.to()));

    if (mSocket >= 0 && mBroadcastSocket >= 0) {
        mThreadIp = std::thread{&DhcpServer::threadStart, this, mSocket};
        mThreadBroadcast =
            std::thread{&DhcpServer::threadStart, this, mBroadcastSocket};
    }
}

DhcpServer::~DhcpServer() noexcept {
    if (mThreadIp.joinable())
        mThreadIp.join();

    if (mThreadBroadcast.joinable())
        mThreadBroadcast.join();
}

std::optional<DhcpPacket> DhcpServer::receivePacket(int socket) const noexcept {
    std::array<char, 512> buffer;
    sockaddr_in address;
    socklen_t socklen;

    auto bytes = recvfrom(
        socket, buffer.begin(), buffer.size(), MSG_WAITALL,
        reinterpret_cast<sockaddr *>(&address), &socklen);

    if (bytes <= 0)
        return std::nullopt;

    RawType raw{buffer.data(), static_cast<size_t>(bytes)};
    auto packet = DhcpPacket::deserialize(raw);

    return packet;
}

void DhcpServer::sendPacket(const DhcpPacket * packet) const noexcept {
    auto raw = packet->serialize();

    sockaddr_in sockaddr;
    std::memset(&sockaddr, 0, sizeof(sockaddr));
    sockaddr.sin_family = AF_INET;
    sockaddr.sin_port = mConfig.clientPort.net();
    sockaddr.sin_addr.s_addr = packet->ipAddress().net();
    logInfo(ipToString(packet->ipAddress()));

    packet->print();
    sendto(
        mSocket, raw.data(), raw.size(), 0,
        reinterpret_cast<struct sockaddr *>(&sockaddr), sizeof(sockaddr));
}

NetInt<uint32_t> defineLeaseTime(
    const Config & config,
    const DhcpPacket * packet) {
    auto clientLeaseTime = packet->getLeaseTime();
    if (clientLeaseTime.has_value())
        return std::min(*clientLeaseTime, config.maxLeaseTime);
    return config.defaultLeaseTime;
}

void DhcpServer::onDiscover(DhcpPacket * packet) noexcept {
    auto clientId = packet->clientId();
    IpType requestedIp;

    if (mClients.has(clientId))
        requestedIp = mClients.get(clientId)->ip;
    else
        requestedIp = packet->requestedIp();

    if (auto yiaddr = mAllocator.reserve(requestedIp); yiaddr != UNDEFINED_IP) {
        packet->yiaddr = yiaddr;
    } else {
        logInfo("No free ip to reserve");
        return;
    }

    offer(packet);
}

void DhcpServer::onRequest(DhcpPacket * packet) noexcept {
    auto serverId = packet->getServerId();
    if (serverId != UNDEFINED_IP && serverId != mId) {
        logInfo("Client chooses another server");
        nack(packet);
        return;
    }

    auto clientId = packet->clientId();
    auto client = mClients.getOrNew(clientId);
    if (serverId == UNDEFINED_IP) {
        if (client->xid != packet->xid) {
            logInfo("Client sends info to another server");
            return;
        }
    }

    auto requestedIp = packet->requestedIp();
    if (requestedIp == 0) {
        logInfo("Client requests wrong ip");
        nack(packet);
        return;
    }

    auto leasedIp = LeasedIp{&mAllocator, requestedIp};
    if (leasedIp == 0) {
        logInfo("No free ip");
        nack(packet);
        return;
    }

    auto leaseTime = client->timer.remainingTime();
    if (packet->xid == client->xid && leaseTime == 0)
        leaseTime = defineLeaseTime(mConfig, packet);

    client->xid = packet->xid;
    client->ip = std::move(leasedIp);
    client->lastMessageType = packet->messageType();

    if (leaseTime != std::numeric_limits<uint32_t>::max())
        client->timer.lease(leaseTime);

    packet->yiaddr = client->ip;
    ack(packet);
}

void DhcpServer::onRelease(DhcpPacket * packet) noexcept {
    auto client = mClients.get(packet->clientId());
    if (client != nullptr) {
        client->timer.release();
        client->ip = LeasedIp{};
    }
}

void DhcpServer::onDecline(DhcpPacket * packet) noexcept {
    auto client = mClients.get(packet->clientId());
    if (client != nullptr) {
        client->timer.release();

        // permanentally allocate busy ip
        mAllocator.allocate(client->ip);
        client->ip = LeasedIp{};
    }

    logInfo(
        "Client has defined that " + ipToString(client->ip) +
        " is used by someone");
}

void DhcpServer::onInform(DhcpPacket * packet) noexcept {
    auto client = mClients.get(packet->clientId());
    if (client != nullptr) {
        packet->yiaddr = 0;
        ack(packet);
    }
}

void DhcpServer::nack(DhcpPacket * packet) noexcept {
    preparePacket(packet);
    packet->secs = 0;
    packet->ciaddr = 0;
    packet->yiaddr = 0;
    packet->clearOptions();
    packet->setMessageType(MessageType::DHCPNACK);
    packet->sname.clear();
    sendPacket(packet);
}

void DhcpServer::ack(DhcpPacket * packet) noexcept {
    preparePacket(packet);
    packet->setMessageType(MessageType::DHCPACK);
    sendPacket(packet);
}

void DhcpServer::offer(DhcpPacket * packet) noexcept {
    preparePacket(packet);
    packet->setMessageType(MessageType::DHCPOFFER);
    packet->ciaddr = 0;
    sendPacket(packet);
}

void DhcpServer::preparePacket(DhcpPacket * packet) noexcept {
    auto leaseTime = packet->getLeaseTime();
    packet->clearOptions();
    packet->op = 2;
    packet->siaddr = 0;
    packet->setServerId(mId);
    packet->setDnsServer(stringToIp(mConfig.dnsServer));
    packet->setSubnetMask(stringToIp(mConfig.mask));
    packet->setRouter(stringToIp(mConfig.router));
    packet->setBroadcast(stringToIp("255.255.255.255"));
    packet->setT1(1500);
    packet->setT2(2200);

    packet->setLeaseTime(3200);
    // if (leaseTime.has_value())
    // packet->setLeaseTime(*leaseTime);
}

void DhcpServer::threadStart(int socket) noexcept {
    while (!mStopped) {
        auto packetOption = receivePacket(socket);
        if (!packetOption.has_value() || packetOption->op == 2)
            continue;

        std::lock_guard lock{mMutex};
        auto packet = &packetOption.value();
        packet->print();
        switch (packet->messageType()) {
            case MessageType::DHCPDISCOVER: onDiscover(packet); break;
            case MessageType::DHCPREQUEST: onRequest(packet); break;
            case MessageType::DHCPRELEASE: onRelease(packet); break;
            case MessageType::DHCPDECLINE: onDecline(packet); break;
            case MessageType::DHCPINFORM: onInform(packet); break;
            default: continue;
        }
    }
    close(socket);
}

void DhcpServer::stop() noexcept {
    mStopped.store(true);
}

}  // namespace dhcp
