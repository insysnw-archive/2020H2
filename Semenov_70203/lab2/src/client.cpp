#include "dhcp/client.h"

#include <algorithm>

namespace dhcp {

Client::Client(const RawType & id) noexcept : mId{id} {}

bool Client::operator==(const Client & other) const noexcept {
    return mId == other.mId;
}

RawType Client::id() const noexcept {
    return mId;
}

Client * ClientManager::get(const RawType & id) noexcept {
    auto clientIterator = std::find_if(
        mClients.begin(), mClients.end(),
        [&id](const Client & client) { return client.id() == id; });

    if (clientIterator != mClients.end())
        return clientIterator.base();

    return nullptr;
}

Client * ClientManager::newClient(const RawType & id) noexcept {
    if (!has(id)) {
        mClients.emplace_back(id);
        return &mClients.back();
    }
    return get(id);
}

void ClientManager::clear() noexcept {
    for (auto & client : mClients)
        if (!client.lease.isActive()) {
            auto removeFrom =
                std::remove(mClients.begin(), mClients.end(), client);
            mClients.erase(removeFrom, mClients.end());
        }
}

bool ClientManager::has(const RawType & id) const noexcept {
    return std::any_of(
        mClients.begin(), mClients.end(),
        [&id](const Client & client) { return id == client.id(); });
}

}  // namespace dhcp
