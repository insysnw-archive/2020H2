#include "tunnel.hpp"

#include <paket.hpp>

#include "settings.hpp"

namespace ktlo::chat {

void tunnel::head(std::int32_t & id, std::int32_t & size) {
	int offset;
	do {
		offset = handtruth::pakets::read_varint(size, input.data(), input.size());
		if (offset == -1) {
			input.asize(1);
			sock->read(input.data() + input.size() - 1, 1);
		} else {
			break;
		}
	} while (true);
	std::int32_t max_size = settings.max_paket_size;
	if (max_size != -1 && size > max_size)
		throw bad_request("the maximum allowed packet size was reached");
	size += offset;
	std::size_t begin = input.size();
	if (begin < std::size_t(size)) {
		input.size(size);
		sock->read(input.data() + begin, size - begin);
	}
	int sz = handtruth::pakets::read_varint(id, input.data() + offset, input.size() - offset);
	if (sz == -1)
		throw bad_request("malformed paket format");
}

} // namespace ktlo::chat
