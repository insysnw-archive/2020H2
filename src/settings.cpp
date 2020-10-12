#include "settings.hpp"

#include <cstring>
#include <stdexcept>
#include <limits>
#include <random>

namespace ktlo::chat {

settings_t settings;

const char * get_arg_value(char const **& it, const char ** end) {
	const char * curr = *it;
	while(char c = *(curr++)) {
		if (c == '=') {
			return curr;
		}
	}
	if (++it != end) {
		return *it;
	}
	throw std::invalid_argument(std::string("program argument '") + *(it - 1) + "' requiers value");
}

bool is_arg_name(const char * arg, const char * name) {
	size_t n = std::strlen(name);
	return !std::strcmp(arg, name) || (!std::strncmp(arg, name, n) && arg[n] == '=');
}

template <typename T>
T assert_arg_range(unsigned long long x, const char * name) {
	constexpr T max_value = std::numeric_limits<T>::max();
	if (x < 0 || x > max_value)
		throw std::invalid_argument(std::string("'") + name + "' value do not fit in range [0:" + std::to_string(max_value) + "]");
	return static_cast<T>(x);
}

void settings_t::parse_args(int argn, const char ** args) {
	const char ** end = args + argn;
	for (const char ** it = args + 1; it != end; it++) {
		const char * curr = *it;
		if (!std::strncmp(curr, "--", 2)) {
			// full name conf
			const char * arg = curr + 2;
			if (is_arg_name(arg, "address"))
				address = get_arg_value(it, end);
			else if (is_arg_name(arg, "port"))
				port = get_arg_value(it, end);
			else if (is_arg_name(arg, "username"))
				username = get_arg_value(it, end);
			else if (is_arg_name(arg, "max-paket-size"))
				max_paket_size = assert_arg_range<decltype(max_paket_size)>(std::atoll(get_arg_value(it, end)), "max-paket-size");
			else if (is_arg_name(arg, "max-client-count"))
				max_client_count = assert_arg_range<decltype(max_client_count)>(std::atoll(get_arg_value(it, end)), "max-client-count");
			else if (is_arg_name(arg, "timeout"))
				timeout = assert_arg_range<decltype(timeout)>(std::atoll(get_arg_value(it, end)), "timeout");
			else if (is_arg_name(arg, "pulse"))
				pulse = assert_arg_range<decltype(pulse)>(std::atoll(get_arg_value(it, end)), "pulse");
			else if (is_arg_name(arg, "single-login"))
				single_login = true;
			else if (is_arg_name(arg, "tcp"))
				type = sock_types::tcp_sock;
			else if (is_arg_name(arg, "unix"))
				type = sock_types::unix_sock;
			else if (is_arg_name(arg, "version"))
				version = true;
			else if (is_arg_name(arg, "help"))
				help = true;
			else if (is_arg_name(arg, "verb"))
				verb = ekutils::str2loglvl(get_arg_value(it, end));
			else if (is_arg_name(arg, "sync"))
				sync = true;
			else
				throw std::invalid_argument("urecognized argument '" + std::string(arg) + "'");
		} else if (*curr == '-') {
			// literal options
			const char * arg = curr + 1;
			while(char c = *(arg++)) {
				if (c == 's')
					single_login = true;
				else if (c == 't')
					type = sock_types::tcp_sock;
				else if (c == 'u')
					type = sock_types::unix_sock;
				else if (c == 'v')
					version = true;
				else if (c == 'h')
					help = true;
				else if (c == 'd')
					verb = ekutils::log_level::debug;
				else if (c == 'S')
					sync = true;
				else
					throw std::invalid_argument(std::string("unrecognized option '") + c + "'");
			}
		} else {
			throw std::invalid_argument("only option-like aruments allowed");
		}
	}
}


const std::string & settings_t::resolve_address() const {
	static const std::string tcp_default = "localhost";
	static const std::string unix_default = "chat.sock";
	if (address.empty()) {
		switch (type) {
		case sock_types::tcp_sock:
			return tcp_default;
		case sock_types::unix_sock:
			return unix_default;
		default:
			throw std::runtime_error("UNREACHABLE");
		}
	} else {
		return address;
	}
}

const std::string & settings_t::resolve_username() const {
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

} // namespace ktlo::chat
