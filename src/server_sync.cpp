#include "server_sync.hpp"

#include <future>
#include <algorithm>

#include <ekutils/tcp_d.hpp>
#include <ekutils/unix_d.hpp>

#include "server_arguments.hpp"

namespace ktlo::chat {

server_sync::server_sync() {
	sock = open_server();
	sock->listen();
	log_info("started server: " + sock->local_endpoint().to_string());
	
	try {
		for (;;) {
			sock_ptr client_sock = sock->accept_virtual();
			if (server_args.max_client_count != -1 && connections.size() >= static_cast<std::uintmax_t>(server_args.max_client_count)) {
				client_sock->close();
				log_warning("maximum client count reached");
				continue;
			}
			ekutils::net::stream_socket_d * client_ptr = client_sock.release();
			std::thread thr([this, client_ptr] {
				sock_ptr client(client_ptr);
				std::list<connection_sync>::iterator iter;
				{
					std::unique_lock lock(connections_mutex);
					connections.emplace_front(*this, std::move(client));
					iter = connections.begin();
				}
				iter->start();
				{
					std::unique_lock lock(connections_mutex);
					connections.erase(iter);
				}
			});
			thr.detach();
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

} // namespace ktlo::chat
