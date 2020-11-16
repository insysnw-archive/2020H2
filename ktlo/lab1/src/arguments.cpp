#include "arguments.hpp"

namespace ktlo::chat {

arguments::transports arguments::protocol() const {
	if (is_unix && is_tcp)
		throw std::invalid_argument("both tcp and udp modes were selected");
	if (is_unix)
		return transports::un;
	else
		return transports::tcp;
}

const std::string & arguments::resolve_address() const {
	static const std::string tcp_default = "localhost";
	static const std::string unix_default = "chat.sock";
	if (address.empty()) {
		switch (protocol()) {
		case transports::tcp:
			return tcp_default;
		case transports::un:
			return unix_default;
		default:
			abort(); // unreachable
		}
	} else {
		return address;
	}
}

arguments::arguments() :
	address(add<string>("address", [this](string & opt) {
		address_hint = &opt.hint;
	})),
	port(add<string>("port", [this](string & opt) {
		port_hint = &opt.hint;
		opt.value = "1338";
	})),
	version(add<flag>("version", [](flag & opt) {
		opt.hint = "show program version";
		opt.c = 'v';
	})),
	help(add<flag>("help", [](flag & opt) {
		opt.hint = "print this help message";
		opt.c = 'h';
	})),
	sync(add<flag>("sync", [](flag & opt) {
		opt.hint = "use threads and blocking code";
		opt.c = 'S';
	})),
	is_tcp(add<flag>("tcp", [](flag & opt) {
		opt.hint = "use tcp socket (default)";
		opt.c = 't';
	})),
	is_unix(add<flag>("unix", [](flag & opt) {
		opt.hint = "use unix socket";
		opt.c = 'u';
	})),
	max_paket_size(add<integer>("max-paket-size", [](integer & opt) {
		opt.hint = "limit paket size";
		opt.value = 3000;
		opt.min = -1;
	}))
{
	allow_positional = false;
	width = 50;
}

arguments * common_args;

} // namespace ktlo::chat
