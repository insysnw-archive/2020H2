#include "records/CNAME.hpp"

#include <yaml-cpp/yaml.h>

#include "database.hpp"
#include "question.hpp"
#include "dnscodec.hpp"
#include "dns_error.hpp"
#include "records/A.hpp"

namespace ktlo::dns::records {

void CNAME::encode(writer & wr) const {
	wr.write_name(alias);
}

void CNAME::decode(reader & rd) {
	alias = rd.read_name();
}

bool CNAME::shoud_answer(const question & q) const {
	return q.qtype == 255 || q.qtype == CNAME::tid || q.qtype == A::tid;
}

std::vector<question_info> CNAME::ask(const question_info & q) const {
	if (q.q.qtype == A::tid)
		return { { question(alias, A::tid, q.q.qclass), q.category } };
	else
		return {};
}

void CNAME::read(const YAML::Node & node, const name & hint) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			const std::string & value = node.as<std::string>();
			alias = context.names().resolve(value, hint);
			break;
		}
		case YAML::NodeType::Map: {
			read(node["name"], hint);
			break;
		}
		default: throw std::runtime_error("wrong YAML node type for CNAME record");
	}
}

std::string CNAME::data_to_string() const {
	return alias.domain();
}

} // ktlo::dns::records
