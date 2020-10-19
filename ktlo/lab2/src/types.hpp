#ifndef TYPES_HEAD_JRTGWESCEDC
#define TYPES_HEAD_JRTGWESCEDC

#include <cinttypes>
#include <string>
#include <string_view>

namespace ktlo {

typedef std::uint8_t byte_t;
typedef std::uint16_t word_t;
typedef std::uint32_t dword_t;
typedef std::uint64_t qword_t;

typedef std::basic_string<byte_t> varbytes;
typedef std::basic_string_view<byte_t> varbytes_view;

} // namespace ktlo

#endif // TYPES_HEAD_JRTGWESCEDC
