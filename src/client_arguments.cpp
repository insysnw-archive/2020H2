#include "client_arguments.hpp"

#include <random>

namespace ktlo::chat {

const std::string & client_arguments::resolve_username() const {
	static const std::vector<std::string> names {
		"BlodPrist", "Virmalinn", "Gilraen", "WLADOSKI", "fdfdd",
		"CRUZERO13", "limono4ka", "fkug", "Didig", "decorm", "deGissar"
	};
	if (username.empty()) {
		std::random_device rd;
		std::mt19937 gen(rd());
		std::uniform_int_distribution<> distrib(0, names.size() - 1);
		return names[distrib(gen)];
	} else {
		return username;
	}
}

client_arguments::client_arguments() :
	username(add<string>("username", [](string & opt) {
		opt.hint = "own username for chat";
	})),
	pulse(add<integer>("pulse", [](integer & opt) {
		opt.hint = "keep alive signal period in milliseconds";
		opt.value = 3000;
		opt.min = 0;
	}))
{
	allow_positional = false;
	*address_hint = "domain, ip address (tcp) or file path (unix) to connect to";
	*port_hint = "tcp port (tcp only)";
}

client_arguments client_args;

} // namespace ktlo::chat
