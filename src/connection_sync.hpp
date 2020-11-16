#ifndef CHAT_CONNECTION_SYNC_HEAD_WDFDBGHMGKHIUJ
#define CHAT_CONNECTION_SYNC_HEAD_WDFDBGHMGKHIUJ

#include <list>
#include <mutex>
#include <thread>

#include <ekutils/idgen.hpp>
#include <ekutils/lateinit.hpp>

#include "tunnel.hpp"
#include "protocol.hpp"
#include "server_shared.hpp"

namespace ktlo::chat {

class server_sync;

class connection_sync final {
	static ekutils::idgen<unsigned> ids;
	server_sync & srv;
	tunnel tube;
	const unsigned id;
	std::string user;
	std::mutex send_mutex;

public:
	connection_sync(server_sync & source, sock_ptr && socket);
	void start();

	ekutils::net::stream_socket_d & socket() noexcept {
		return tube.socket();
	}

	void send(const protocol::chat & chat);

	const std::string & username() const noexcept {
		return user;
	}

	~connection_sync();
};

} // namespace ktlo::chat

#endif // CHAT_CONNECTION_SYNC_HEAD_WDFDBGHMGKHIUJ
