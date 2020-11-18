#include "answer.hpp"

namespace ktlo::dns {

std::string answer::to_string() const {
	return aname.domain() + '\t' + arecord().to_string();
}

} // namespace ktlo::dns
