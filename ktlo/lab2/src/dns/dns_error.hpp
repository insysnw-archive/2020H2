#ifndef DNS_DNS_ERROR_HEAD_QLVKVKFERV
#define DNS_DNS_ERROR_HEAD_QLVKVKFERV

#include <stdexcept>
#include <string>

#include "dns_enum.hpp"

namespace ktlo::dns {

struct dns_error : public std::runtime_error {
	const rcodes rcode;

	dns_error(rcodes code) :
		std::runtime_error(rcode_to_string(code)), rcode(code) {}
	dns_error(rcodes code, const char * str) :
		std::runtime_error(std::string(rcode_to_string(code)) + ": " + str), rcode(code) {}
	dns_error(rcodes code, const std::string & str) :
		std::runtime_error(std::string(rcode_to_string(code)) + ": " + str), rcode(code) {}
};

} // ktlo::dns

#endif // DNS_DNS_ERROR_HEAD_QLVKVKFERV
