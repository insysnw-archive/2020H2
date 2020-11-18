#include "records/MX.hpp"

#include <cassert>
#include <limits>
#include <sstream>

#include <yaml-cpp/yaml.h>

#include "database.hpp"
#include "question.hpp"
#include "dnscodec.hpp"
#include "dns_error.hpp"
#include "records/A.hpp"

namespace ktlo::dns::records {

void MX::encode(writer & wr) const {
    wr.write<word_t>(preference);
	wr.write_name(exchange);
}

void MX::decode(reader & rd) {
    preference = rd.read<word_t>();
    exchange = rd.read_name();
}

std::vector<question_info> MX::ask(const question_info & q) const {
	if (q.q.qtype == MX::tid)
		return { { question(exchange, A::tid, q.q.qclass), q.category } };
	else
		return {};
}

void MX::read(const YAML::Node & node, const name & hint) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			std::stringstream ss(node.as<std::string>());
            ss >> preference;
            std::string value;
            ss >> value;
            exchange = context.names().resolve(value, hint);
			break;
		}
		case YAML::NodeType::Map: {
			if (auto it = node["preference"])
                preference = it.as<word_t>();
            if (auto it = node["exchange"])
                exchange = context.names().resolve(it.as<std::string>(), hint);
			break;
		}
		default: throw std::invalid_argument("wrong YAML node type for MX record");
	}
}

std::string MX::data_to_string() const {
	return std::to_string(preference) + ' ' + exchange.domain();
}

} // ktlo::dns::records
