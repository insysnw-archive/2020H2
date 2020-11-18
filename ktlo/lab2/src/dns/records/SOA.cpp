#include "records/SOA.hpp"

#include <yaml-cpp/yaml.h>

#include <sstream>

#include "database.hpp"
#include "dnscodec.hpp"
#include "dns_error.hpp"

namespace ktlo::dns::records {

void SOA::encode(writer & wr) const {
	wr.write_name(mname);
	wr.write_name(rname);
	wr.write<dword_t>(serial);
	wr.write<dword_t>(refresh);
	wr.write<dword_t>(retry);
	wr.write<dword_t>(expire);
	wr.write<dword_t>(minimum);
}

void SOA::decode(reader & rd) {
	mname = rd.read_name();
	rname = rd.read_name();
	serial = rd.read<dword_t>();
	refresh = rd.read<dword_t>();
	retry = rd.read<dword_t>();
	expire = rd.read<dword_t>();
	minimum = rd.read<dword_t>();
}

void SOA::read(const YAML::Node & node, const name & hint) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			std::stringstream ss(node.as<std::string>());
			std::string value;
			ss >> value;
			mname = context.names().resolve(value, hint);
			ss >> value;
			rname = context.names().resolve(value, hint);
			ss >> serial;
			ss >> refresh;
			ss >> retry;
			ss >> expire;
			ss >> minimum;
			break;
		}
		case YAML::NodeType::Map: {
			mname = context.names().resolve(node["origin"].as<std::string>(), hint);
			rname = context.names().resolve(node["admin"].as<std::string>(), hint);
			serial = node["serial"].as<dword_t>();
			refresh = node["refresh"].as<dword_t>();
			retry = node["retry"].as<dword_t>();
			expire = node["expire"].as<dword_t>();
			minimum = node["minimum"].as<dword_t>();
			break;
		}
		default: throw std::invalid_argument("wrong YAML node type for SOA record");
	}
}

std::string SOA::data_to_string() const {
	return mname.domain() + ' ' + rname.domain() + ' ' +
		std::to_string(serial) + ' ' + std::to_string(refresh) + ' ' +
		std::to_string(retry) + ' ' + std::to_string(expire) + ' ' +
		std::to_string(minimum);
}

} // ktlo::dns::records
