#include <ipv6.hpp>

#include <stdexcept>
#include <cstring>
#include <arpa/inet.h>

#include <ipv6.h>

namespace ktlo::dns {

ipv6 ipv6::parse(const std::string_view & string) {
	ipv6_address_full_t addr;
	std::memset(&addr, 0, sizeof(addr));
	if (!ipv6_from_str(string.data(), string.size(), &addr))
		throw std::runtime_error("ipv6 format error");
	ipv6 result;
	std::memcpy(result.words.data(), addr.address.components, result.words.size() * sizeof(result.words[0]));
	return result;
}

std::string ipv6::to_string() const {
	ipv6_address_full_t addr;
	std::memset(&addr, 0, sizeof(addr));
	addr.flags = IPV6_FLAG_IPV4_EMBED;
	std::memcpy(addr.address.components, words.data(), words.size() * sizeof(words[0]));
	constexpr size_t max_size = INET6_ADDRSTRLEN;
	char output[max_size];
	std::size_t size = ipv6_to_str(&addr, output, max_size);
	return std::string(output, size);
}

} // namespace ktlo::dns
