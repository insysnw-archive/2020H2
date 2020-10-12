#include "server.hpp"

#include <ekutils/tcp_d.hpp>
#include <ekutils/unix_d.hpp>

#include "settings.hpp"

namespace ktlo::chat {

server::server(ekutils::epoll_d & poll) : epoll(poll) {
	switch (settings.type) {
		case settings_t::sock_types::tcp_sock: {
			sock = std::make_unique<ekutils::tcp_listener_d>(settings.resolve_address(), settings.port, ekutils::sock_flags::non_blocking);
			break;
		}
		case settings_t::sock_types::unix_sock: {
			sock = std::make_unique<ekutils::unix_stream_listener_d>(settings.resolve_address(), ekutils::sock_flags::non_blocking);
			break;
		}
		default:
			throw std::runtime_error("UNREACHABLE");
	}
	sock->start();
	log_info("started server: " + std::string(sock->local_endpoint()));
	epoll.add(*sock, [this](const auto &, const auto &) {
		on_accept();
	});
}

void server::broadcast(const std::string & username, std::string && message) {
	protocol::chat chat = protocol::chat::create(username, std::move(message));

	for (connection & client : connections)
		client.send(chat);
}

std::size_t server::there(const std::string & username) const {
	return std::count_if(connections.cbegin(), connections.cend(), [&username](const connection & client) -> bool {
		return client.username() == username;
	});
}

void server::on_accept() {
	if (connections.size() >= settings.max_client_count) {
		sock->accept_virtual()->close();
		log_warning("maximum client count reached");
		return;
	}
	auto & client_sock = connections.emplace_front(*this, sock->accept_virtual()).socket();
	client_sock.set_non_block();
	auto iter = connections.begin();
	using namespace ekutils::actions;
	epoll.add(client_sock, in | out | et | err | rdhup, [this, iter](ekutils::descriptor &, std::uint32_t events) {
		if (iter->closed()) {
			epoll.remove(iter->socket());
			connections.erase(iter);
			return;
		}
		iter->on_event(events);
		if (iter->closed()) {
			epoll.remove(iter->socket());
			connections.erase(iter);
		}
	});
}

} // namespace ktlo::chat
