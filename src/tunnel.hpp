#ifndef CHAT_TUNNEL_HEAD_REGERGREKFEPB
#define CHAT_TUNNEL_HEAD_REGERGREKFEPB

#include <memory>

#include <ekutils/expandbuff.hpp>
#include <ekutils/stream_socket_d.hpp>

#include "bad_request.hpp"
#include "client_shared.hpp"

namespace ktlo::chat {

class tunnel final {
	sock_ptr sock;
	ekutils::expandbuff input, output;
public:
	explicit tunnel(sock_ptr && socket) : sock(std::move(socket)) {}
	void head(std::int32_t & id, std::int32_t & size);
	template <typename P>
	bool paket_read(P & packet);
	template <typename P>
	void paket_write(const P & packet);
	ekutils::stream_socket_d & socket() noexcept {
		return *sock;
	}
};

template <typename P>
bool tunnel::paket_read(P & packet) {
	std::int32_t id, size;
	head(id, size);
	int s = packet.read(input.data(), size);
	if (std::int32_t(s) != size)
		throw bad_request("packet format error " + std::to_string(s) + " " + std::to_string(size));
	input.move(size);
	return true;
}

template <typename P>
void tunnel::paket_write(const P & packet) {
	std::size_t size = 10 + packet.size();
	output.size(size);
	int s = packet.write(output.data() + output.size() - size, size);
#	ifdef DEBUG
		if (s < 0)
			throw "IMPOSSIBLE SITUATION";
#	endif
	sock->write(output.data(), s);
}

} // namespace ktlo::chat

#endif // CHAT_TUNNEL_HEAD_REGERGREKFEPB
