#include "question.hpp"

#include "record.hpp"

namespace ktlo::dns {

std::string question::to_string() const {
	return qname.domain() + "\t\t" + class_to_string(qclass) + '\t' + record::tname(qtype);
}

} // namespace ktlo::dns
