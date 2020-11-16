#include "client_shared.hpp"

#include <ekutils/resolver.hpp>
#include <ekutils/unix_d.hpp>

#include "client_arguments.hpp"

namespace ktlo::chat {

sock_ptr connect_client() {
	switch (client_args.protocol()) {
		case arguments::transports::tcp: {
			auto targets = ekutils::net::resolve(client_args.resolve_address(), client_args.port, ekutils::net::protocols::tcp);
			return ekutils::net::connect_any(targets.begin(), targets.end());
		}
		case arguments::transports::un: {
			ekutils::net::un::endpoint target(client_args.resolve_address());
			auto result = std::make_unique<ekutils::net::client_stream_unix_socket_d>();
			result->connect(target);
			return result;
		}
		default: {
			abort(); // unreachable
		}
	}
}

} // namespace ktlo::chat
