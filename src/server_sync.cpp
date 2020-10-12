#include "server_sync.hpp"

#include <future>
#include <algorithm>

#include <ekutils/tcp_d.hpp>
#include <ekutils/unix_d.hpp>

#include "settings.hpp"

namespace ktlo::chat {

server_sync::server_sync() {
	switch (settings.type) {
		case settings_t::sock_types::tcp_sock: {
			sock = std::make_unique<ekutils::tcp_listener_d>(settings.resolve_address(), settings.port);
			break;
		}
		case settings_t::sock_types::unix_sock: {
			sock = std::make_unique<ekutils::unix_stream_listener_d>(settings.resolve_address());
			break;
		}
		default:
			throw std::runtime_error("UNREACHABLE");
	}
	sock->start();
	log_info("started server: " + std::string(sock->local_endpoint()));
	
	try {
		for (;;) {
			auto client_sock = sock->accept_virtual();
			if (connections.size() >= settings.max_client_count) {
				client_sock->close();
				log_warning("maximum client count reached");
				continue;
			}
			std::unique_lock lock(connections_mutex);
			connection_sync & connection = connections.emplace_front(*this, std::move(client_sock));
			connection.start(connections.begin());
		}
	} catch (const std::exception & e) {
		log_error("server socket error");
		log_error(e);
	}
}

void server_sync::broadcast(const std::string & username, std::string && message) {
	std::shared_lock lock(connections_mutex);
	
	protocol::chat chat = protocol::chat::create(username, std::move(message));

	for (connection_sync & client : connections)
		client.send(chat);
}

std::size_t server_sync::there(const std::string & username) const {
	std::shared_lock lock(connections_mutex);
	return std::count_if(connections.cbegin(), connections.cend(), [&username](const connection_sync & client) -> bool {
		return client.username() == username;
	});
}

void server_sync::forget(connection_sync & connection) {
	std::unique_lock lock(connections_mutex);
	connections.erase(connection.me());
}

} // namespace ktlo::chat
