#include "records/AAAA.hpp"

#include <cstring>

#include <yaml-cpp/yaml.h>

#include "dnscodec.hpp"

namespace ktlo::dns::records {

void AAAA::encode(writer & wr) const {
	wr.write_bytes(varbytes_view(address.data.data(), address.data.size()));
}

void AAAA::decode(reader & rd) {
	if (rd.pending() != address.data.size())
		throw dns_error(rcodes::format_error, "wrong AAAA record size");
	varbytes_view bytes = rd.read_bytes(address.data.size());
	std::memcpy(address.data.data(), bytes.data(), address.data.size());
}

void AAAA::read(const YAML::Node & node, const name & hint) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			address = ekutils::net::ipv6::address(node.as<std::string>());
			break;
		}
		case YAML::NodeType::Map: {
			read(node["address"], hint);
			break;
		}
		default: throw std::invalid_argument("wrong YAML node type for AAAA record");
	}
}

std::string AAAA::data_to_string() const {
	return address.to_string();
}

} // ktlo::dns::records
