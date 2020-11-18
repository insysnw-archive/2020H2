#include "records/NS.hpp"

#include <yaml-cpp/yaml.h>

#include "database.hpp"
#include "dnscodec.hpp"
#include "dns_error.hpp"
#include "records/A.hpp"

namespace ktlo::dns::records {

void NS::encode(writer & wr) const {
	wr.write_name(nsdname);
}

void NS::decode(reader & rd) {
    nsdname = rd.read_name();
}

std::vector<question_info> NS::ask(const question_info & q) const {
	return { { question(nsdname, A::tid, q.q.qclass), answer_categories::additional } };
}

void NS::read(const YAML::Node & node, const name & hint) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			const std::string & value = node.as<std::string>();
            nsdname = context.names().resolve(value, hint);
			break;
		}
		case YAML::NodeType::Map: {
			read(node["name"], hint);
			break;
		}
		default: throw std::invalid_argument("wrong YAML node type for NS record");
	}
}

std::string NS::data_to_string() const {
	return nsdname.domain();
}

} // ktlo::dns::records
