#include "packet.hpp"

#include <bitset>

#include "codec.hpp"

namespace ktlo::ntp {

void packet::read(const varbytes_view & src) {
	reader rd { src };
	byte_t flags = rd.read<byte_t>();
	leap = leaps((flags >> 6) & 0b11u);
	version = (flags >> 3) & 0b111u;
	mode = modes(flags & 0b111u);
	stratum = rd.read<byte_t>();
	poll = rd.read<byte_t>();
	precision = rd.read<byte_t>();
	rootdelay = rd.read<dword_t>();
	rootdisp = rd.read<dword_t>();
	refcode = rd.read_raw<dword_t>();
	refid[4] = '\0';
	rd >> reference;
	rd >> origin;
	rd >> receive;
	rd >> transmit;
	/*
	while (rd.pending() > 20) {
		// extensions
		word_t ftype = rd.read<word_t>();
		word_t length = rd.read<word_t>();
		varbytes_view fdata = rd.read_bytes(length - sizeof(ftype) - sizeof(length));
		extensions.emplace_back(ftype, fdata);
	}
	keyid = rd.read<dword_t>();
	*/
}

void packet::write(varbytes & dest) {
	dest.clear();
	writer wr { dest };
	byte_t flags = 0;
	flags |= byte_t(leap) << 6;
	flags |= version << 3;
	flags |= byte_t(mode);
	wr.write<byte_t>(flags);
	wr.write<byte_t>(stratum);
	wr.write<byte_t>(poll);
	wr.write<byte_t>(precision);
	wr.write<dword_t>(rootdelay);
	wr.write<dword_t>(rootdisp);
	wr.write<dword_t>(refcode);
	wr << reference;
	wr << origin;
	wr << receive;
	wr << transmit;
	for (const extension & ext : extensions) {
		wr.write<word_t>(ext.ftype);
		wr.write<word_t>(ext.value.size() + sizeof(word_t) + sizeof(word_t));
		wr.write_bytes(ext.value);
	}
}

} // namespace ktlo::ntp
