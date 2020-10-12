#ifndef CHAT_CLIENT_SYNC_HEAD_PSOFBGBVEDC
#define CHAT_CLIENT_SYNC_HEAD_PSOFBGBVEDC

#include <mutex>

#include <ekutils/stream_socket_d.hpp>

#include "tunnel.hpp"
#include "client_shared.hpp"

namespace ktlo::chat {

class client_sync {
	tunnel tube;
	std::mutex mutex;

public:
	explicit client_sync() : client_sync(connect_client()) {}

private:
	client_sync(sock_ptr && sock);
};

} // namespace ktlo::chat

#endif // CHAT_CLIENT_SYNC_HEAD_PSOFBGBVEDC
