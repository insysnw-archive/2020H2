#include "records/SRV.hpp"

#include <sstream>

#include <yaml-cpp/yaml.h>

#include "database.hpp"
#include "dnscodec.hpp"

namespace ktlo::dns::records {

void SRV::encode(writer & wr) const {
	wr.write<word_t>(priority);
	wr.write<word_t>(weight);
	wr.write<word_t>(port);
	wr.write_name(target);
}

void SRV::decode(reader & rd) {
	priority = rd.read<word_t>();
	weight = rd.read<word_t>();
	port = rd.read<word_t>();
	target = rd.read_name();
}

void SRV::read(const YAML::Node & node, const name & hint) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			std::stringstream ss(node.as<std::string>());
			ss >> priority;
			ss >> weight;
			ss >> port;
			std::string value;
			ss >> value;
			target = context.names().resolve(value, hint);
			break;
		}
		case YAML::NodeType::Map: {
			priority = node["priority"].as<word_t>();
			weight = node["weight"].as<word_t>();
			port = node["port"].as<word_t>();
			target = context.names().resolve(node["target"].as<std::string>(), hint);
			break;
		}
		default: throw std::invalid_argument("wrong YAML node type for SRV record");
	}
}

std::string SRV::data_to_string() const {
	return std::to_string(priority) + ' ' + std::to_string(weight) + ' ' + std::to_string(port) + ' ' + target.domain();
}

} // ktlo::dns::records
