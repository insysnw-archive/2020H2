#include "records/A.hpp"

#include <sstream>
#include <cctype>

#include <yaml-cpp/yaml.h>

#include <ekutils/socket_d.hpp>

#include "dnscodec.hpp"

namespace ktlo::dns::records {

void A::encode(writer & wr) const {
	wr.write_raw<dword_t>(address.data);
}

void A::decode(reader & rd) {
	if (rd.pending() != 4)
		throw dns_error(rcodes::format_error, "wrong A record size");
	address = rd.read_raw<dword_t>();
}

void A::read(const YAML::Node & node, const name & hint) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			address = ekutils::net::ipv4::address(node.as<std::string>());
			break;
		}
		case YAML::NodeType::Map: {
			read(node["address"], hint);
			break;
		}
		default: throw std::invalid_argument("wrong YAML node type for A record");
	}
}

std::string A::data_to_string() const {
	return address.to_string();
}

} // ktlo::dns::records
