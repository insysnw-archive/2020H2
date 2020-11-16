#include "server_arguments.hpp"

namespace ktlo::chat {

server_arguments::server_arguments() :
	max_client_count(add<integer>("max-client-count", [](integer & opt) {
		opt.hint = "limit simultaneous connections count";
		opt.value = -1;
		opt.min = -1;
	})),
	timeout(add<integer>("timeout", [](integer & opt) {
		opt.hint = "timeout in milliseconds for any actions from client";
		opt.value = 5000;
		opt.min = -1;
	})),
	single_login(add<flag>("single-login", [](flag & opt) {
		opt.hint = "only one client with the same name can";
		opt.c = 's';
	})),
	verb(add<variant<ekutils::log_level>>("verb", [](variant<ekutils::log_level> & opt) {
		opt.hint = "logging level";
		opt.value = ekutils::log_level::info;
		opt.variants = {
			{ "none", ekutils::log_level::none },
			{ "fatal", ekutils::log_level::fatal },
			{ "error", ekutils::log_level::error },
			{ "warning", ekutils::log_level::warning },
			{ "info", ekutils::log_level::info },
			{ "verbose", ekutils::log_level::verbose },
			{ "debug", ekutils::log_level::debug }
		};
	})),
	debug(add<flag>("debug", [](flag & opt) {
		opt.hint = "use debug log level";
		opt.c = 'd';
	}))
{
	*address_hint = "domain, ip address (tcp) or file path (unix) to bind";
	*port_hint = "tcp port to bind (tcp only)";
}

server_arguments server_args;

} // namespace ktlo::chat
