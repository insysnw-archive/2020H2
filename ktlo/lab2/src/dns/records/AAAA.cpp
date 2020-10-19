#include "records/AAAA.hpp"

#include <cstring>

#include <yaml-cpp/yaml.h>

#include "dns_error.hpp"
#include "zones.hpp"
#include "codec.hpp"

namespace ktlo::dns::records {

void AAAA::encode(varbytes & data) const {
	ktlo::writer wr { data };
	for (word_t word : address.words)
		wr.write<word_t>(word);
}

void AAAA::decode(const varbytes_view & data) {
	if (data.size() != 16)
		throw dns_error(rcodes::format_error, "wrong AAAA record size");
	ktlo::reader rd { data };
	for (word_t & word : address.words)
		word = rd.read<word_t>();
}

void AAAA::read(const YAML::Node & node, const name & zone) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			address = ipv6::parse(node.as<std::string>());
			break;
		}
		case YAML::NodeType::Map: {
			read(node["address"], zone);
			break;
		}
		default: throw zone_error(node.Mark(), "wrong YAML node type for AAAA record");
	}
}

std::string AAAA::data_to_string() const {
	return address.to_string();
}

} // ktlo::dns::records
