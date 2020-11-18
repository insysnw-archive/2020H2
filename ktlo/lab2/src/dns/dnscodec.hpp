#ifndef DNS_DNSCODEC_HEAD_QQDVKGBOSCSDYGH
#define DNS_DNSCODEC_HEAD_QQDVKGBOSCSDYGH

#include <unordered_map>
#include <memory>

#include "codec.hpp"
#include "namez.hpp"

namespace ktlo::dns {

class reader final : public ktlo::reader {
	typedef std::shared_ptr<std::unordered_map<std::size_t, name>> names_cache;
	namez & ns;
	names_cache names;
	const byte_t * start;

	reader(namez & n, const varbytes_view & d, const names_cache & cache, const byte_t * s) :
		ktlo::reader { d }, ns(n), names(cache), start(s) {}

public:
	reader(namez & n, const varbytes_view & d) : ktlo::reader { d },
		ns(n), names(std::make_unique<std::unordered_map<std::size_t, name>>()), start(ptr()) {}

	name read_name();

	reader record_reader(std::size_t size) {
		return reader(ns, read_bytes(size), names, start);
	}
};

class writer final : public ktlo::writer {
	typedef std::shared_ptr<std::unordered_map<name, std::size_t>> names_cache;
	names_cache names;
	std::size_t border;

	writer(varbytes & d, const names_cache & cache) :
		ktlo::writer { d }, names(cache), border(d.size()) {}

public:
	writer(varbytes & d) : ktlo::writer { d },
		names(std::make_shared<std::unordered_map<name, std::size_t>>()), border(0) {}

	void write_name(const name & n);

	writer record_writer() {
		return writer(buffer(), names);
	}

	std::size_t size() const noexcept {
		return ktlo::writer::size() - border;
	}
};

} // ktlo::dns

#endif // DNS_DNSCODEC_HEAD_QQDVKGBOSCSDYGH
