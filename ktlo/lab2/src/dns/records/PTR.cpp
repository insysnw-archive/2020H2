#include "records/PTR.hpp"

#include <yaml-cpp/yaml.h>

#include "dnscodec.hpp"
#include "dns_error.hpp"
#include "zones.hpp"

namespace ktlo::dns::records {

void PTR::encode(varbytes & data) const {
	writer wr(data);
	wr.write_name(ptrdname);
}

void PTR::decode(const varbytes_view & data) {
	reader rd(gloabl_names, data);
    ptrdname = rd.read_name();
}

void PTR::read(const YAML::Node & node, const name & zone) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			const std::string & value = node.as<std::string>();
            ptrdname = gloabl_names.resolve(value, zone);
			break;
		}
		case YAML::NodeType::Map: {
			read(node["name"], zone);
			break;
		}
		default: throw zone_error(node.Mark(), "wrong YAML node type for PTR record");
	}
}

std::string PTR::data_to_string() const {
	return ptrdname.domain();
}

} // ktlo::dns::records
