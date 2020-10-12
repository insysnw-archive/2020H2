#ifndef CHAT_CLIENT_SHARED_HEAD_EFGTGIHUJYHV
#define CHAT_CLIENT_SHARED_HEAD_EFGTGIHUJYHV

#include <memory>

#include <ekutils/stream_socket_d.hpp>

namespace ktlo::chat {

typedef std::unique_ptr<ekutils::stream_socket_d> sock_ptr;

sock_ptr connect_client();

} // namespace ktlo::chat

#endif // CHAT_CLIENT_SHARED_HEAD_EFGTGIHUJYHV
