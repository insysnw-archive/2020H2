#ifndef DNS_DNSCODEC_HEAD_QQDVKGBOSCSDYGH
#define DNS_DNSCODEC_HEAD_QQDVKGBOSCSDYGH

#include <unordered_map>

#include "codec.hpp"
#include "namez.hpp"

namespace ktlo::dns {

class reader final : public ktlo::reader {
	namez & ns;
	std::unordered_map<std::size_t, name> names;
	const byte_t * start;

public:
	reader(namez & n, const varbytes_view & d) : ktlo::reader { d }, ns(n), start(data.data()) {}

	name read_name();
};

class writer final : public ktlo::writer {
	std::unordered_map<name, std::size_t> names;
	const bool compress_names;

public:
	writer(varbytes & d, bool compress = false) : ktlo::writer { d }, compress_names(compress) {}

	void write_name(const name & n);
};

} // ktlo::dns

#endif // DNS_DNSCODEC_HEAD_QQDVKGBOSCSDYGH
