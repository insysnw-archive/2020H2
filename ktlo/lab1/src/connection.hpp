#ifndef CHAT_CONNECTION_HEAD_WDFDBGHMGKHIUJ
#define CHAT_CONNECTION_HEAD_WDFDBGHMGKHIUJ

#include <ekutils/idgen.hpp>
#include <ekutils/epoll_d.hpp>

#include "gate.hpp"
#include "protocol.hpp"
#include "server_shared.hpp"

namespace ktlo::chat {

class server;

class connection final {
	static ekutils::idgen<unsigned> ids;
	server & srv;
	gate tube;
	enum class states {
		handshake, chatting, closed
	} state;
	unsigned id;
	std::string user;
	int timeout_task;

public:
	connection(server & source, sock_ptr && socket);

	ekutils::net::stream_socket_d & socket() noexcept {
		return tube.socket();
	}

	bool closed() const noexcept {
		return state == states::closed;
	}

	void send(const protocol::chat & chat);

	void on_event(std::uint32_t events);

	const std::string & username() const noexcept {
		return user;
	}

private:
	bool process_request();
	bool process_handshake();
	bool process_chatting();

	void reset_timeout();
	void on_timeout();

	void disconnect() noexcept;

public:
	~connection();
};

} // namespace ktlo::chat

#endif // CHAT_CONNECTION_HEAD_WDFDBGHMGKHIUJ
