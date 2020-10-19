#include "records/NS.hpp"

#include <yaml-cpp/yaml.h>

#include "dnscodec.hpp"
#include "dns_error.hpp"
#include "zones.hpp"

namespace ktlo::dns::records {

void NS::encode(varbytes & data) const {
	writer wr(data);
	wr.write_name(nsdname);
}

void NS::decode(const varbytes_view & data) {
	reader rd(gloabl_names, data);
    nsdname = rd.read_name();
}

void NS::read(const YAML::Node & node, const name & zone) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			const std::string & value = node.as<std::string>();
            nsdname = gloabl_names.resolve(value, zone);
			break;
		}
		case YAML::NodeType::Map: {
			read(node["name"], zone);
			break;
		}
		default: throw zone_error(node.Mark(), "wrong YAML node type for NS record");
	}
}

std::string NS::data_to_string() const {
	return nsdname.domain();
}

} // ktlo::dns::records
