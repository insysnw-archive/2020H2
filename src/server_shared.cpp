#include "server_shared.hpp"

#include <ekutils/unix_d.hpp>

#include "server_arguments.hpp"

namespace ktlo::chat {

listener_ptr open_server(std::uint32_t flags) {
	switch (server_args.protocol()) {
		case arguments::transports::tcp: {
			auto targets = ekutils::net::resolve(server_args.resolve_address(), server_args.port, ekutils::net::protocols::tcp);
			return ekutils::net::bind_stream_any(targets.begin(), targets.end(), flags);
		}
		case arguments::transports::un: {
			auto sock = std::make_unique<ekutils::net::server_stream_unix_socket_d>();
			sock->bind(ekutils::net::un::endpoint(server_args.resolve_address()), flags);
			return sock;
		}
		default:
			abort(); // unreachable
	}
}

} // namespace ktlo::chat
