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

namespace dhcp {

DhcpServer::DhcpServer(const Config & config) noexcept
    : mStopped{false}, mConfig{config}, mAllocator{config.range} {
    mSocket = bindedSocket(config);
    mId = IpType::fromString(config.address);

    auto broadcastConfig = config;
    broadcastConfig.address = "255.255.255.255";
    mBroadcastSocket = bindedSocket(broadcastConfig);

    logInfo(
        "Ip from " + config.range.from().toString() + " to " +
        config.range.to().toString());

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

    packet->print();
    sendto(
        mSocket, raw.data(), raw.size(), 0,
        reinterpret_cast<struct sockaddr *>(&sockaddr), sizeof(sockaddr));
}

net32 defineLeaseTime(const Config & config, const DhcpPacket * packet) {
    auto clientLeaseTime = packet->getLeaseTime();
    if (clientLeaseTime.has_value())
        return std::min(*clientLeaseTime, config.maxLeaseTime);
    return config.defaultLeaseTime;
}

void DhcpServer::onDiscover(DhcpPacket * packet) noexcept {
    auto clientId = packet->clientId();
    IpType requestedIp;

    if (mClients.has(clientId))
        requestedIp = mClients.get(clientId)->lease.ip();
    else if (auto ip = packet->requestedIp(); ip != UNDEFINED_IP)
        requestedIp = *ip;

    if (auto yiaddr = mAllocator.reserve(requestedIp); yiaddr.isActive()) {
        packet->yiaddr = yiaddr.ip();
    } else {
        logInfo("No free ip to reserve");
        return;
    }

    offer(packet);
}

void DhcpServer::onRequest(DhcpPacket * packet) noexcept {
    auto serverId = packet->getServerId();
    auto clientId = packet->clientId();

    if (serverId != mId && serverId != UNDEFINED_IP) {
        logInfo("Client sent packet to another server");
        nack(packet);
        return;
    }

    Lease lease;
    if (serverId == mId) {
        lease = onRequestFromNewClient(packet);
    } else if (mClients.has(clientId)) {
        if (packet->ciaddr == 0)
            lease = onRequestFromKnownClient(packet);
        else
            lease = onRequestUpdateLeaseTime(packet);
    } else {
        logInfo("There is no info about this client");
        nack(packet);
        return;
    }

    if (!lease.isActive()) {
        nack(packet);
        logInfo("Cannot lease an ip address");
        return;
    }

    auto client = mClients.get(clientId);
    client->xid = packet->xid;
    client->lease = std::move(lease);
    client->lastMessageType = packet->messageType();
    packet->yiaddr = client->lease.ip();

    ack(packet);
}

Lease DhcpServer::onRequestUpdateLeaseTime(DhcpPacket * packet) noexcept {
    auto clientId = packet->clientId();
    auto client = mClients.get(clientId);
    auto requestedTime = defineLeaseTime(mConfig, packet);

    if (client->lease.isActive()) {
        auto ip = client->lease.ip();
        client->lease.release();
        return mAllocator.allocate(requestedTime, ip);
    }

    return Lease{};
}

Lease DhcpServer::onRequestFromNewClient(DhcpPacket * packet) noexcept {
    IpType clientIp;
    auto clientId = packet->clientId();
    auto requestedIp = packet->requestedIp();

    // Allocate previous ip address from history
    if (mClients.has(clientId)) {
        auto client = mClients.get(clientId);
        if (mAllocator.isFree(client->lease.ip()))
            clientIp = client->lease.ip();
    } else {
        clientIp = requestedIp.value_or(0);
    }

    auto leaseTime = defineLeaseTime(mConfig, packet);
    auto lease = mAllocator.allocate(leaseTime, clientIp);

    if (!lease.isActive()) {
        logInfo("No free ip for new client");
        return lease;
    }

    mClients.newClient(clientId);
    return lease;
}

Lease DhcpServer::onRequestFromKnownClient(DhcpPacket * packet) noexcept {
    auto serverId = packet->getServerId();
    auto clientId = packet->clientId();
    auto client = mClients.get(clientId);
    auto requestedIp = packet->requestedIp();

    if (!requestedIp.has_value()) {
        logInfo("No requested ip");
        return Lease{};
    }

    if (client->lease.isActive() && client->lease.ip() == *requestedIp) {
        return std::move(client->lease);
    }

    auto leaseTime = defineLeaseTime(mConfig, packet);
    auto lease = mAllocator.allocate(leaseTime, *requestedIp);

    if (lease.ip() != requestedIp) {
        logInfo("Requested ip is currenlty allocated");
        lease.release();
    }

    return lease;
}

void DhcpServer::onRelease(DhcpPacket * packet) noexcept {
    auto client = mClients.get(packet->clientId());
    if (client != nullptr)
        client->lease.release();
}

void DhcpServer::onDecline(DhcpPacket * packet) noexcept {
    auto client = mClients.get(packet->clientId());
    if (client != nullptr) {
        auto busyIp = client->lease.ip();
        client->lease.release();
        mAllocator.allocate(busyIp);
        // permanentally allocate busy ip
    }

    logInfo(
        "Client has defined that " + client->lease.ip().toString() +
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
    packet->setServerId(mId);
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
    auto leaseTime = defineLeaseTime(mConfig, packet);
    packet->clearOptions();

    packet->op = 2;
    packet->siaddr = 0;
    packet->setServerId(mId);
    packet->setDnsServer(IpType::fromString(mConfig.dnsServer));
    packet->setSubnetMask(IpType::fromString(mConfig.mask));
    packet->setRouter(IpType::fromString(mConfig.router));
    packet->setBroadcast(IpType::fromString("255.255.255.255"));
    packet->setT1(leaseTime * mConfig.t1);
    packet->setT2(leaseTime * mConfig.t2);
    packet->setLeaseTime(leaseTime);
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
