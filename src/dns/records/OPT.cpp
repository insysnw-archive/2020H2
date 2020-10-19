#include "records/OPT.hpp"

#include <sstream>
#include <cctype>

#include <yaml-cpp/yaml.h>

#include "dns_error.hpp"
#include "zones.hpp"
#include "base64.hpp"

namespace ktlo::dns::records {

void OPT::encode(varbytes & data) const {
	data = buffer;
}

void OPT::decode(const varbytes_view & data) {
	buffer = data;
}

void OPT::read(const YAML::Node & node, const name &) {
	throw zone_error(node.Mark(), "not implemented");
}

std::string OPT::data_to_string() const {
	return base64(buffer);
}

} // ktlo::dns::records
