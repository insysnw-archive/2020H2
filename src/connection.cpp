#include "connection.hpp"

#include <chrono>

#include <ekutils/log.hpp>

#include "settings.hpp"
#include "server.hpp"
#include "bad_request.hpp"

namespace ktlo::chat {

ekutils::idgen<unsigned> connection::ids;

connection::connection(server & source, sock_ptr && socket) :
	srv(source), tube(std::move(socket)), state(states::handshake), id(ids.next()), timeout_task(-1)
{
	log_info("new client connected #" + std::to_string(id));
	reset_timeout();
}

void connection::send(const protocol::chat & chat) {
	if (state == states::chatting)
		tube.paket_write(chat);
}

void connection::on_event(std::uint32_t events) {
	namespace actions = ekutils::actions;
	try {
		if (events & actions::in) {
			tube.receive();
			while (process_request());
		}
		if (events & (actions::rdhup | actions::hup)) {
			// just disconnect event from client
			disconnect();
		}
		if (events & actions::err) {
			// disconnect with async error
			int err = errno;
			log_debug("async connection error received from client #" + std::to_string(id));
			throw std::system_error(std::make_error_code(std::errc(err)), "async error");
		}
		if (events & actions::out) {
			// out information is ready to be send
			tube.send();
		}
		reset_timeout();
	} catch (const std::exception & e) {
		log_error("connection error on client #" + std::to_string(id));
		log_error(e);
		disconnect();
	}
}

#define phead(id, size) \
	do { if (!tube.head(id, size)) return false; } while (false)

#define precv(p) \
	do { if (!tube.paket_read(p)) return false; } while (false)

bool connection::process_request() {
	switch (state) {
	case states::handshake:
		return process_handshake();
	case states::chatting:
		return process_chatting();
	default:
		throw bad_request("UNREACHABLE");
	}
}

bool connection::process_handshake() {
	std::int32_t id, size;
	phead(id, size);
	protocol::handshake hs;
	precv(hs);
	if (hs.version() != protocol::version)
		throw bad_request("client #" + std::to_string(id) + " sent wrong protocol version");
	user = std::move(hs.username());
	if (settings.single_login && srv.there(user) != 1) {
		// check multiple login
		throw bad_request("user \"" + user + "\" already connected (client #" + std::to_string(id) + ")");
	}
	state = states::chatting;
	return true;
}

bool connection::process_chatting() {
	std::int32_t id, size;
	phead(id, size);
	if (id == protocol::ids::noop) {
		precv(protocol::noop);
		return true;
	}
	protocol::tell tell;
	precv(tell);
	srv.broadcast(user, std::move(tell.message()));
	return true;
}

void connection::reset_timeout() {
	using namespace std::chrono_literals;
	if (timeout_task != -1)
		srv.poll().refuse(timeout_task);
	timeout_task = srv.poll().later(std::chrono::milliseconds(settings.timeout), [this]() {
		on_timeout();
	});
}

void connection::on_timeout() {
	log_info("no signal from client #" + std::to_string(id) + ", disconnectiong...");
	disconnect();
}

void connection::disconnect() noexcept {
	state = states::closed;
	log_info("disconnect client #" + std::to_string(id));
}

connection::~connection() {
	if (timeout_task != -1)
		srv.poll().refuse(timeout_task);
}

} // namespace ktlo::chat
