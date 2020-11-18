#include "records/OPT.hpp"

#include <sstream>
#include <cctype>

#include <yaml-cpp/yaml.h>

#include "dns_error.hpp"
#include "base64.hpp"
#include "dnscodec.hpp"

namespace ktlo::dns::records {

void OPT::encode(writer & wr) const {
	wr.write_bytes(buffer);
}

void OPT::decode(reader & rd) {
	buffer = rd.read_all();
}

void OPT::read(const YAML::Node &, const name &) {
	throw std::runtime_error("not implemented");
}

std::string OPT::data_to_string() const {
	return base64(buffer);
}

} // ktlo::dns::records
