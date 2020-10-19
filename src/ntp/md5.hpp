#ifndef NTP_MD5_HEAD_EDEFQCNGDRDRFV
#define NTP_MD5_HEAD_EDEFQCNGDRDRFV

#include <array>

#include "types.hpp"

namespace ktlo::ntp {

struct digest_t final {
	union {
		std::array<byte_t, 16> bytes;
		struct {
			dword_t a;
			dword_t b;
			dword_t c;
			dword_t d;
		};
	};

	std::string to_string() const;
};

digest_t md5(const varbytes_view & data);

} // namespace ktlo::ntp

#endif // NTP_MD5_HEAD_EDEFQCNGDRDRFV
