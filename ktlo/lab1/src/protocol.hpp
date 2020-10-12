#ifndef CHAT_PROTOCOL_HEAD_QPCOSKKKFK
#define CHAT_PROTOCOL_HEAD_QPCOSKKKFK

#define PAKET_LIB_EXT
#include <paket.hpp>

namespace ktlo::chat::protocol {

using namespace handtruth::pakets;

namespace ids {
	enum {
		handshake,
		noop,
		tell,
		chat
	};
} // ids

constexpr unsigned version = 1u;

struct handshake : public paket<ids::handshake, fields::zint<unsigned>, fields::string, fields::string> {
	fname(version, 0)
	fname(address, 1)
	fname(username, 2)

	handshake() {
		version() = protocol::version;
	}
};

inline struct : public paket<ids::noop> {} noop;

struct tell : public paket<ids::tell, fields::string> {
	fname(message, 0)
};

struct chat : public paket<ids::chat, fields::int64, fields::string, fields::string> {
	fname(time, 0)
	fname(username, 1)
	fname(message, 2)

	static chat create(const std::string & username, std::string && message);
};

} // ktlo::chat::protocol

#endif // CHAT_PROTOCOL_HEAD_QPCOSKKKFK
