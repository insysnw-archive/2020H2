#include "server.hpp"

#include <ekutils/tcp_d.hpp>
#include <ekutils/unix_d.hpp>

#include "server_arguments.hpp"

namespace ktlo::chat {

server::server(ekutils::epoll_d & poll) : epoll(poll) {
	sock = open_server(ekutils::net::socket_flags::non_block);
	sock->listen();
	log_info("started server: " + sock->local_endpoint().to_string());
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
	if (server_args.max_client_count != -1 && connections.size() >= static_cast<std::uintmax_t>(server_args.max_client_count)) {
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
