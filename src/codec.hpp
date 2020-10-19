#ifndef CODEC_HEAD_PEVVRGRTGDDD
#define CODEC_HEAD_PEVVRGRTGDDD

#include <stdexcept>

#include "types.hpp"

namespace ktlo {

inline bool is_big_endian_f() {
    union {
        std::uint32_t i;
        char c[4];
    } bint = {0x01020304};

    return bint.c[0] == 1; 
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

struct writer {
	varbytes & data;

	template <typename int_t>
	void write_raw(int_t integer) {
		data.append(reinterpret_cast<const byte_t *>(&integer), sizeof(int_t));
	}

	template <>
	void write_raw<byte_t>(byte_t integer) {
		data += integer;
	}

	template <typename int_t>
	void write(int_t integer) {
		write_raw(hton(integer));
	}

	template <>
	void write<byte_t>(byte_t integer) {
		data += integer;
	}

	void write_bytes(const varbytes_view & bytes) {
		data += bytes;
	}

	std::size_t size() const noexcept {
		return data.size();
	}
};

struct reader {
	varbytes_view data;

	template <typename int_t>
	int_t read_raw() {
		constexpr std::size_t size = sizeof(int_t);
		if (data.size() < size)
			throw std::out_of_range("end of data packet");
		int_t result = *reinterpret_cast<const int_t *>(data.data());
		data.remove_prefix(size);
		return result;
	}
	
	template <typename int_t>
	int_t read() {
		return ntoh(read_raw<int_t>());
	}

	template <>
	byte_t read<byte_t>() {
		return read_raw<byte_t>();
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
};

} // namespace ktlo

#endif // CODEC_HEAD_PEVVRGRTGDDD
