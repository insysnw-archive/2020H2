#include "gate.hpp"

#include <paket.hpp>

#include "arguments.hpp"

namespace ktlo::chat {

bool gate::head(std::int32_t & id, std::int32_t & size) const {
	int s = handtruth::pakets::head(input.data(), input.size(), size, id);
	if (s == -1)
		return false;
	if (size < 0)
		throw bad_request("packet size is lower than 0");
	auto max_size = common_args->max_paket_size;
	if (max_size != -1 && size > max_size)
		throw bad_request("the maximum allowed packet size was reached");
	size += s;
	return std::size_t(size) <= input.size();
}

void gate::receive() {
	std::size_t avail = sock->avail();
	std::size_t old = input.size();
	input.asize(avail);
	sock->read(input.data() + old, avail);
}

void gate::send() {
	if (output.size() == 0)
		return;
	int written = sock->write(output.data(), output.size());
	if (written == -1)
		return;
	output.move(written);
}

} // namespace ktlo::chat
