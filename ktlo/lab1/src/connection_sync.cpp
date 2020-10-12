#include "connection_sync.hpp"

#include "server_sync.hpp"
#include "protocol.hpp"
#include "settings.hpp"
#include "bad_request.hpp"

namespace ktlo::chat {

ekutils::idgen<unsigned> connection_sync::ids;

connection_sync::connection_sync(server_sync & source, sock_ptr && socket) :
	srv(source), tube(std::move(socket)), id(ids.next())
{
	log_info("new client connected #" + std::to_string(id));
}

void connection_sync::start(const std::list<connection_sync>::iterator & rm) {
	remover = rm;
	thr = std::thread([this]() {
		socket().set_timeout(std::chrono::milliseconds(settings.timeout));
		try {
			{
				protocol::handshake hs;
				tube.paket_read(hs);
				if (hs.version() != protocol::version)
					throw bad_request("client #" + std::to_string(id) + " sent wrong protocol version");
				user = std::move(hs.username());
			}
			if (settings.single_login && srv.there(user) != 1) {
				// check multiple login
				throw bad_request("user \"" + user + "\" already connected (client #" + std::to_string(id) + ")");
			}
			for (;;) {
				std::int32_t id, size;
				tube.head(id, size);
				switch (id) {
					case protocol::ids::noop: {
						tube.paket_read(protocol::noop);
						break;
					}
					case protocol::ids::tell: {
						protocol::tell msg;
						tube.paket_read(msg);
						srv.broadcast(user, std::move(msg.message()));
						break;
					}
					default: {
						throw bad_request("unknown paket id: #" + std::to_string(id) + " (client #" + std::to_string(this->id) + ")");
					}
				}
			}
		} catch (const std::system_error & e) {
			std::errc ecode = std::errc(e.code().value());
			if (ecode != std::errc::broken_pipe && ecode != std::errc::bad_file_descriptor) {
				log_error("connection error on client #" + std::to_string(id));
				log_error(e);
			}
		} catch (const std::exception & e) {
			log_error("error on client #" + std::to_string(id));
			log_error(e);
		} catch (...) {
			log_error("unknown error on client #" + std::to_string(id));
		}
		srv.forget(*this);
	});
	thr.detach();
}

void connection_sync::send(const protocol::chat & chat) {
	//std::lock_guard lock(send_mutex);
	try {
	tube.paket_write(chat);
	} catch (const std::system_error & e) {
		if (e.code().value() == int(std::errc::broken_pipe))
			socket().close();
		else
			throw e;
	}
}

connection_sync::~connection_sync() {
	log_info("disconnect client #" + std::to_string(id));
}

} // namespace ktlo::chat
