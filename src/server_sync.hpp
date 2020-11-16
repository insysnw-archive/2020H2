#ifndef CHAT_SERVER_SYNC_HEAD_QEDFEPOBGHMNYG
#define CHAT_SERVER_SYNC_HEAD_QEDFEPOBGHMNYG

#include <list>
#include <memory>
#include <shared_mutex>

#include "connection_sync.hpp"

namespace ktlo::chat {

class server_sync final {
	listener_ptr sock;
	std::list<connection_sync> connections;
	mutable std::shared_mutex connections_mutex;

public:
	server_sync();
	void broadcast(const std::string & username, std::string && message);
	std::size_t there(const std::string & username) const;

private:
	void on_accept();

};

} // namespace ktlo::chat

#endif // CHAT_SERVER_SYNC_HEAD_QEDFEPOBGHMNYG
