#ifndef DNS_IPV6_HEAD_DESFVNWQAFRB
#define DNS_IPV6_HEAD_DESFVNWQAFRB

#include <array>
#include <cinttypes>
#include <string_view>

#include "types.hpp"

namespace ktlo::dns {

struct ipv6 {
	std::array<word_t, 8> words;
	
	static ipv6 parse(const std::string_view & string);

	std::string to_string() const;
};

} // namespace ktlo::dns

#endif // DNS_IPV6_HEAD_DESFVNWQAFRB
