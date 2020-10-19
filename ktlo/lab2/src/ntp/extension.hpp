#ifndef NTP_EXTENSION_HEAD_SKVFOBDFFVY
#define NTP_EXTENSION_HEAD_SKVFOBDFFVY

#include <cinttypes>

#include "types.hpp"

namespace ktlo::ntp {

struct extension final {
	word_t ftype;
	varbytes value;

	extension(word_t t, const varbytes_view & d) : ftype(t), value(d) {}
};

} // namespace ktlo::ntp

#endif // NTP_EXTENSION_HEAD_SKVFOBDFFVY
