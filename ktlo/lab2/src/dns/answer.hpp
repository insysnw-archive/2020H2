#ifndef DNS_ANSWER_HEAD_PBTRHGAREGW
#define DNS_ANSWER_HEAD_PBTRHGAREGW

#include <memory>

#include "namez.hpp"
#include "record.hpp"

namespace ktlo::dns {

struct answer {
	name aname;
	const record & arecord;

	answer(const name & n, const record & r) : aname(n), arecord(r) {}

	std::string to_string() const;
};

} // ktlo::dns

#endif // DNS_ANSWER_HEAD_PBTRHGAREGW
