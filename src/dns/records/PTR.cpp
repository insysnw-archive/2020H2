#include "records/PTR.hpp"

#include <yaml-cpp/yaml.h>

#include "database.hpp"
#include "dnscodec.hpp"

namespace ktlo::dns::records {

void PTR::encode(writer & wr) const {
	wr.write_name(ptrdname);
}

void PTR::decode(reader & rd) {
    ptrdname = rd.read_name();
}

void PTR::read(const YAML::Node & node, const name & hint) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			const std::string & value = node.as<std::string>();
            ptrdname = context.names().resolve(value, hint);
			break;
		}
		case YAML::NodeType::Map: {
			read(node["name"], hint);
			break;
		}
		default: throw std::invalid_argument("wrong YAML node type for PTR record");
	}
}

std::string PTR::data_to_string() const {
	return ptrdname.domain();
}

} // ktlo::dns::records
