#include "dnscodec.hpp"

#include "dns_error.hpp"

namespace ktlo::dns {

name reader::read_name() {
	std::uint8_t byte = read<byte_t>();
	switch (byte >> 6) {
		case 0b00u: {
			std::ptrdiff_t ptr = data.data() - start - 1;
			varbytes_view label_raw = read_bytes(byte);
			std::string_view label(reinterpret_cast<const char *>(label_raw.data()), label_raw.size());
			name result = byte ? ns.add(label, read_name()) : ns.root();
			names.emplace(ptr, result);
			return result;
		}
		case 0b11u: {
			auto iter = names.find(((byte & 0b111111u) << 8) | read<byte_t>());
			if (iter == names.end())
				throw dns_error(rcodes::format_error, "forward name ptr");
			return iter->second;
		}
		default: throw dns_error(rcodes::not_implemented, "unknown label octet type");
	}
}

void writer::write_name(const name & n) {
	auto iter = names.find(n);
	if (!compress_names || iter == names.end()) {
		std::size_t offset = data.size();
		const std::string & label = n.label();
		write<byte_t>(static_cast<byte_t>(label.size()));
		varbytes_view label_raw(reinterpret_cast<const byte_t *>(label.data()), label.size());
		write_bytes(label_raw);
		if (offset <= 0x3FFF)
			names.emplace(n, offset);
		if (!n.is_root())
			write_name(n.parent());
	} else {
		std::size_t offset = iter->second;
		assert(offset <= 0x3FFF);
		write<byte_t>(0b11000000u | (static_cast<byte_t>(offset >> 8)));
		write<byte_t>(offset & 0xFF);
	}
}

} // ktlo::dns
