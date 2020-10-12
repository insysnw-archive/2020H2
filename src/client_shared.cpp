#include "client_shared.hpp"

#include <ekutils/tcp_d.hpp>
#include <ekutils/unix_d.hpp>

#include "settings.hpp"

namespace ktlo::chat {

sock_ptr connect_client() {
	sock_ptr sock;
	switch (settings.type) {
	case settings_t::sock_types::tcp_sock:
		sock = std::make_unique<ekutils::tcp_socket_d>(
			ekutils::connection_info::resolve(settings.resolve_address(), settings.port)
		);
		break;
	case settings_t::sock_types::unix_sock:
		sock = std::make_unique<ekutils::unix_stream_socket_d>(
			settings.resolve_address()
		);
		break;
	default:
		throw std::runtime_error("UNREACHABLE");
	}
	return sock;
}

} // namespace ktlo::chat
