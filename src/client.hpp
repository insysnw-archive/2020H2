#ifndef CHAT_CLIENT_HEAD_PSOFBGBVEDC
#define CHAT_CLIENT_HEAD_PSOFBGBVEDC

#include <ekutils/stream_socket_d.hpp>
#include <ekutils/epoll_d.hpp>
#include <ekutils/reader.hpp>
#include <ekutils/timer_d.hpp>

#include "gate.hpp"
#include "client_shared.hpp"

namespace ktlo::chat {

class client {
	ekutils::epoll_d & epoll;
	ekutils::reader input;
	gate tube;
	ekutils::timer_d timer;

public:
	explicit client(ekutils::epoll_d & poll) : client(poll, connect_client()) {}

private:
	client(ekutils::epoll_d & poll, sock_ptr && sock);
	void on_input(std::uint32_t events);
	bool process_input();
	void on_event(std::uint32_t events);
	bool process_response();
	void disconnect_remote();
	void disconnect_self();

public:
	~client();
};

} // namespace ktlo::chat

#endif // CHAT_CLIENT_HEAD_PSOFBGBVEDC
