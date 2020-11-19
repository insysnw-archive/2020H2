#include "dhcp/client.h"

#include <algorithm>

namespace dhcp {

Client::Client() noexcept : timer{&ip} {}
bool Client::operator==(const Client & other) const noexcept {
    return chaddr == other.chaddr;
}

Client & ClientManager::get(const RawType & chaddr) noexcept {
    auto clientIterator = std::find_if(
        mClients.begin(), mClients.end(),
        [&chaddr](const Client & client) { return client.chaddr == chaddr; });

    if (clientIterator != mClients.end())
        return *clientIterator;

    Client client;
    client.chaddr = chaddr;
    mClients.emplace_back(std::move(client));
    return mClients.back();
}

Client & ClientManager::operator[](const RawType & chaddr) noexcept {
    return get(chaddr);
}

void ClientManager::clear() noexcept {
    for (auto & client : mClients)
        if (client.timer.isStopped()) {
            auto removeFrom =
                std::remove(mClients.begin(), mClients.end(), client);
            mClients.erase(removeFrom, mClients.end());
        }
}

}  // namespace dhcp
