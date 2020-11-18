#ifndef CODEC_HEAD_PEVVRGRTGDDD
#define CODEC_HEAD_PEVVRGRTGDDD

#include <stdexcept>

#include "types.hpp"

namespace ktlo {

inline bool is_big_endian_f() {
    union {
        std::uint16_t i;
        char c[2];
    } bint = { 0x01000 };
    return bint.c[0];
}

static const bool is_big_endian = is_big_endian_f();

template <typename int_t>
int_t hton(int_t integer) {
	if (is_big_endian) {
		return integer;
	} else {
		constexpr std::size_t size = sizeof(int_t);
		union {
			int_t full;
			byte_t bytes[size];
		} number { integer };
		for (std::size_t i = 0; i < size/2; i++) {
			byte_t tmp = number.bytes[i];
			number.bytes[i] = number.bytes[size - i - 1];
			number.bytes[size - i - 1] = tmp;
		}
		return number.full;
	}
}

template <typename int_t>
int_t ntoh(int_t integer) {
	return hton(integer);
}

struct writer;

template <typename int_t>
int_t & write_raw(writer & wr, int_t integer);

template <typename int_t>
void write(writer & wr, int_t integer);

class writer {
	varbytes & data;

public:
	writer(varbytes & it) : data(it) {}

	template <typename int_t>
	int_t & write_raw(int_t integer = 0) {
		return ktlo::write_raw<int_t>(*this, integer);
	}

	template <typename int_t>
	void write(int_t integer = 0) {
		ktlo::write<int_t>(*this, integer);
	}

	varbytes_view write_bytes(const varbytes_view & bytes) {
		varbytes_view result(data.data() + data.size(), bytes.size());
		data += bytes;
		return result;
	}

	std::size_t size() const noexcept {
		return data.size();
	}

	varbytes & buffer() noexcept {
		return data;
	}

	template <typename int_t>
	friend int_t & write_raw(writer & wr, int_t integer);

	template <typename int_t>
	friend int_t & write(writer & wr, int_t integer);
};

template <typename int_t>
int_t & write_raw(writer & wr, int_t integer) {
	std::size_t offset = wr.size();
	wr.data.append(reinterpret_cast<const byte_t *>(&integer), sizeof(int_t));
	return *reinterpret_cast<int_t *>(wr.data.data() + offset);
}

template <>
inline byte_t & write_raw<byte_t>(writer & wr, byte_t integer) {
	wr.data += integer;
	return wr.data[wr.size() - sizeof(byte_t)];
}

template <typename int_t>
void write(writer & wr, int_t integer) {
	write_raw(wr, hton(integer));
}

template <>
inline void write<byte_t>(writer & wr, byte_t integer) {
	write_raw<byte_t>(wr, integer);
}

struct reader;

template <typename int_t>
int_t read_raw(reader & rd);

template <typename int_t>
int_t read(reader & rd);

class reader {
	varbytes_view data;

public:
	explicit reader(const varbytes_view & it) : data(it) {}

	template <typename int_t>
	int_t read_raw() {
		return ktlo::read_raw<int_t>(*this);
	}

	template <typename int_t>
	int_t read() {
		return ktlo::read<int_t>(*this);
	}

	varbytes_view read_bytes(std::size_t size) {
		if (data.size() < size)
			throw std::out_of_range("end of data packet");
		varbytes_view bytes = data.substr(0, size);
		data.remove_prefix(size);
		return bytes;
	}

	std::size_t pending() const noexcept {
		return data.size();
	}

	varbytes_view read_all() {
		return read_bytes(pending());
	}

	const byte_t * ptr() const noexcept {
		return data.data();
	}

	template <typename int_t>
	friend int_t read_raw(reader & rd);
	
	template <typename int_t>
	friend int_t read(reader & rd);
};

template <typename int_t>
int_t read_raw(reader & rd) {
	constexpr std::size_t size = sizeof(int_t);
	if (rd.data.size() < size)
		throw std::out_of_range("end of data packet");
	int_t result = *reinterpret_cast<const int_t *>(rd.data.data());
	rd.data.remove_prefix(size);
	return result;
}

template <typename int_t>
int_t read(reader & rd) {
	return ntoh(rd.read_raw<int_t>());
}

template <>
inline byte_t read<byte_t>(reader & rd) {
	return read_raw<byte_t>(rd);
}

} // namespace ktlo

#endif // CODEC_HEAD_PEVVRGRTGDDD
