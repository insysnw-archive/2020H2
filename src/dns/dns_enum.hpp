#ifndef DNS_DNS_ENUM_HEAD_QQPXPVHRGFDSD
#define DNS_DNS_ENUM_HEAD_QQPXPVHRGFDSD

#include <cinttypes>

namespace ktlo::dns {

typedef std::uint16_t record_tids;

enum class record_classes : std::uint16_t {
	unknown, IN, CS, CH, HS
};

const char * class_to_string(record_classes rclass);

enum class opcodes : std::uint8_t {
	QUERY,
	IQUERY,
	STATUS,
};

const char * opcode_to_string(opcodes opcode);

enum class rcodes : std::uint8_t {
	no_error,
	format_error,
	server_failure,
	name_error,
	not_implemented,
	refused,
};

const char * rcode_to_string(rcodes rcode);

} // ktlo::dns

#endif // DNS_DNS_ENUM_HEAD_QQPXPVHRGFDSD
