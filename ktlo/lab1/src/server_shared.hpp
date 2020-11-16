#ifndef SERVER_SHARED_HEAD_WDGJNYFVDRBB
#define SERVER_SHARED_HEAD_WDGJNYFVDRBB

#include <ekutils/resolver.hpp>

namespace ktlo::chat {

typedef std::unique_ptr<ekutils::net::stream_server_socket_d> listener_ptr;

listener_ptr open_server(std::uint32_t flags = ekutils::net::socket_flags::nothing);

} // namespace ktlo::chat

#endif // SERVER_SHARED_HEAD_WDGJNYFVDRBB
