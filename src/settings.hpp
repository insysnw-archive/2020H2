#ifndef CHAT_SETTINGS_HEAD_FARCFSQEDFEESD
#define CHAT_SETTINGS_HEAD_FARCFSQEDFEESD

#include <vector>
#include <string>
#include <cinttypes>

#include <ekutils/log.hpp>

namespace ktlo::chat {

struct settings_t {
	std::string address = "";
	std::string port = "1338";
	std::string username = "";
	std::uint32_t max_paket_size = 3000u;
	std::size_t max_client_count = -1;
	std::uint64_t timeout = 5000;
	std::uint64_t pulse = 3000;
	bool single_login = false;
	bool version = false;
	bool help = false;
	bool sync = false;

	ekutils::log_level verb = ekutils::log_level::info;
	
	enum class sock_types {
		tcp_sock, unix_sock
	} type = sock_types::tcp_sock;

	void parse_args(int argc, const char ** argv);

	const std::string & resolve_address() const;
	const std::string & resolve_username() const;
};

extern settings_t settings;

} // namespace ktlo::chat

#endif // CHAT_SETTINGS_HEAD_FARCFSQEDFEESD
