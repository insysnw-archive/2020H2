#include "protocol.hpp"

#include <chrono>

#include <ekutils/log.hpp>

namespace ktlo::chat::protocol {

chat chat::create(const std::string & username, std::string && message) {
	using namespace std::chrono;
	auto time = system_clock::now();
	std::int64_t epoch = duration_cast<seconds>(time.time_since_epoch()).count();
	log_verbose("chat: [" + username + "] " + message);

	protocol::chat chat;
	chat.time() = epoch;
	chat.username() = username;
	chat.message() = std::move(message);
	return chat;
}

} // namespace ktlo::chat
