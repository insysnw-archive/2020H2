#include "records/MX.hpp"

#include <cassert>
#include <limits>
#include <sstream>

#include <yaml-cpp/yaml.h>

#include "dnscodec.hpp"
#include "dns_error.hpp"
#include "zones.hpp"
#include "records/A.hpp"

namespace ktlo::dns::records {

void MX::encode(varbytes & data) const {
	writer wr(data);
    wr.write<word_t>(preference);
	wr.write_name(exchange);
}

void MX::decode(const varbytes_view & data) {
	reader rd(gloabl_names, data);
    preference = rd.read<word_t>();
    exchange = rd.read_name();
}

std::vector<question> MX::ask(const question & q) const {
	if (q.qtype == MX::tid)
		return { question(exchange, A::tid, q.qclass) };
	else
		return {};
}

void MX::read(const YAML::Node & node, const name & zone) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			std::stringstream ss(node.as<std::string>());
            ss >> preference;
            std::string value;
            ss >> value;
            exchange = gloabl_names.resolve(value, zone);
			break;
		}
		case YAML::NodeType::Map: {
			if (auto it = node["preference"])
                preference = it.as<word_t>();
            if (auto it = node["exchange"])
                exchange = gloabl_names.resolve(it.as<std::string>(), zone);
			break;
		}
		default: throw zone_error(node.Mark(), "wrong YAML node type for MX record");
	}
}

std::string MX::data_to_string() const {
	return std::to_string(preference) + ' ' + exchange.domain();
}

} // ktlo::dns::records
