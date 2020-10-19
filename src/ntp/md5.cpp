#include "md5.hpp"

#include <type_traits>

#include "md5_block.hpp"

namespace ktlo::ntp {

static constexpr std::string_view hex_digits = "0123456789abcdef";

std::string digest_t::to_string() const {
	std::string result;
	result.reserve(32);
	for (unsigned i = 0; i < 16; ++i) {
		result.push_back(hex_digits[bytes[i] >> 4]);
		result.push_back(hex_digits[bytes[i] & 0xF]);
	}
	return result;
}

static const std::array<byte_t, 64> shifts {
	7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,
	5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,
	4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,
	6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21,
};

static const std::array<dword_t, 64> weights {
	0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
	0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
	0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
	0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
	0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
	0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
	0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
	0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
	0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
	0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
	0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
	0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
	0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
	0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
	0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
	0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391,
};

template <typename int_t>
int_t rot(int_t integer, int cnt) {
	return (integer << cnt) | (integer >> (sizeof(int_t)*8 - cnt));
}

digest_t md5(const varbytes_view & data) {
	varbytes_view bytes = data;
	dword_t a0 = 0x67452301u;
	dword_t b0 = 0xefcdab89u;
	dword_t c0 = 0x98badcfeu;
	dword_t d0 = 0x10325476u;
	qword_t size = data.size()*8;

	std::size_t count = bytes.size()/64u;
	for (std::size_t i = 0; i <= count; i++) {
		block_t block { bytes.substr(i*64u, 64u), size };
		dword_t a = a0;
		dword_t b = b0;
		dword_t c = c0;
		dword_t d = d0;
		for (unsigned j = 0; j < 64; j++) {
			dword_t f, g;
			switch (j >> 4) {
				case 0:
					f = (b & c) | (~b & d);
					g = j;
					break;
				case 1:
					f = (d & b) | (~d & c);
					g = (5*j + 1) % 16;
					break;
				case 2:
					f = b ^ c ^ d;
					g = (3*j + 5) % 16;
					break;
				case 3:
					f = c ^ (b | ~d);
					g = (7*j) % 16;
					break;
			}
			dword_t temp = d;
			d = c;
			c = b;
			b += rot<dword_t>(f + a + weights[j] + block[g], shifts[j]);
			a = temp;
		}
		a0 += a;
		b0 += b;
		c0 += c;
		d0 += d;
	}
	digest_t result;
	result.a = htod(a0);
	result.b = htod(b0);
	result.c = htod(c0);
	result.d = htod(d0);
	return result;
}

} // namespace ktlo::ntp
