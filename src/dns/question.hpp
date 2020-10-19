#ifndef DNS_QUESTION_HEAD_EPQOVJNNXKKKSK
#define DNS_QUESTION_HEAD_EPQOVJNNXKKKSK

#include "dns_enum.hpp"
#include "namez.hpp"

namespace ktlo::dns {

struct question {
	name qname;
	record_tids qtype;
	record_classes qclass;

	question(const name & n, record_tids t, record_classes c) : qname(n), qtype(t), qclass(c) {}

	std::string to_string() const;
};

} // namespace ktlo::dns

#endif // DNS_QUESTION_HEAD_EPQOVJNNXKKKSK
