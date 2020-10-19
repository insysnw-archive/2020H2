#ifndef NTP_MD5_BLOCK_HEAD_EQQDFEDC
#define NTP_MD5_BLOCK_HEAD_EQQDFEDC

#include "types.hpp"

namespace ktlo::ntp {

constexpr dword_t dtoh(dword_t integer) {
	return integer;
}

constexpr dword_t htod(dword_t integer) {
	return integer;
}

struct block_t {
	varbytes_view bytes;
	qword_t allsize;

	dword_t operator[](std::size_t i) const {
		typedef std::make_signed_t<std::size_t> ssize_t;
		ssize_t offset = bytes.size() - (i+1) * sizeof(dword_t);
		if (offset >= 0) {
			return dtoh(*reinterpret_cast<const dword_t *>(bytes.data() + i * sizeof(dword_t)));
		} else {
			std::size_t virt = 64 - bytes.size();
			std::size_t uoffset = -offset;
			if (uoffset == virt - sizeof(dword_t)) {
				return static_cast<dword_t>(allsize);
			}
			if (uoffset == virt) {
				return static_cast<dword_t>(allsize >> (sizeof(dword_t) * 8));
			}
			if (uoffset <= sizeof(dword_t)) {
				union {
					dword_t result;
					byte_t b[sizeof(dword_t)];
				} number { 0x00000000u };
				std::size_t one_pos = sizeof(dword_t) - uoffset;
				number.b[one_pos] = 0x80;
				for (std::size_t j = 0; j < one_pos; ++j)
					number.b[j] = bytes[bytes.size() - one_pos + j];
				return dtoh(number.result);
			}
			return 0;
		}
	}
};

} // namespace ktlo::ntp

#endif // NTP_MD5_BLOCK_HEAD_EQQDFEDC
