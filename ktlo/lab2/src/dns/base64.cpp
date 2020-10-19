#include "base64.hpp"

#include <array>

namespace ktlo::dns {

static constexpr std::string_view symbols =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZ"
	"abcdefghijklmnopqrstuvwxyz"
	"0123456789+/";

std::string base64(const varbytes_view & data) {
	std::string result;
	std::size_t size = data.size();
	std::size_t div = size / 3;
	std::size_t rem = size % 3;
	std::size_t result_size = (div + (rem ? 1 : 0)) * 4;
	result.reserve(result_size);
	for (std::size_t i = 0; i <= size - 3; i += 3) {
		byte_t a = data[i];
		byte_t b = data[i + 1];
		byte_t c = data[i + 2];
		std::array<char, 4> block;
		block[0] = symbols[a >> 2];
		block[1] = symbols[((a & 0b11) << 4) | (b >> 4)];
		block[2] = symbols[((b & 0xF) << 2) | (c >> 6)];
		block[3] = symbols[c & 0x3F];
		result.append(block.data(), 4);
	}
	if (rem) {
		std::array<char, 4> block;
		switch (rem) {
		case 2: {
			byte_t a = data[size - 2];
			byte_t b = data[size - 1];
			block[0] = symbols[a >> 2];
			block[1] = symbols[((a & 0b11) << 4) | (b >> 4)];
			block[2] = symbols[(b & 0xF) << 2];
			block[3] = '=';
			break;
		}
		case 1: {
			byte_t a = data[size - 1];
			block[0] = symbols[a >> 2];
			block[1] = symbols[(a & 0b11) << 4];
			block[2] = '=';
			block[3] = '=';
			break;
		}
		default:
			break;
		}
		result.append(block.data(), 4);
	}
	return result;
}

} // namespace ktlo::dns
