#include "records/CNAME.hpp"

#include <yaml-cpp/yaml.h>

#include "dnscodec.hpp"
#include "dns_error.hpp"
#include "zones.hpp"
#include "records/A.hpp"

namespace ktlo::dns::records {

void CNAME::encode(varbytes & data) const {
	writer wr(data);
	wr.write_name(this->data);
}

void CNAME::decode(const varbytes_view & data) {
	reader rd(gloabl_names, data);
	this->data = rd.read_name();
}

bool CNAME::shoud_answer(const question & q) const {
	return q.qtype == 255 || q.qtype == CNAME::tid || q.qtype == A::tid;
}

std::vector<question> CNAME::ask(const question & q) const {
	if (q.qtype == A::tid)
		return { question(data, A::tid, q.qclass) };
	else
		return {};
}

void CNAME::read(const YAML::Node & node, const name & zone) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			const std::string & value = node.as<std::string>();
			data = gloabl_names.resolve(value, zone);
			break;
		}
		case YAML::NodeType::Map: {
			read(node["name"], zone);
			break;
		}
		default: throw zone_error(node.Mark(), "wrong YAML node type for CNAME record");
	}
}

std::string CNAME::data_to_string() const {
	return data.domain();
}

} // ktlo::dns::records
