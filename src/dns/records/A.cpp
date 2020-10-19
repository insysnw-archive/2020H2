#include "records/A.hpp"

#include <sstream>
#include <cctype>

#include <yaml-cpp/yaml.h>

#include "dns_error.hpp"
#include "zones.hpp"

namespace ktlo::dns::records {

void A::encode(varbytes & data) const {
	data.append(reinterpret_cast<const byte_t *>(&addr_dword), 4);
}

void A::decode(const varbytes_view & data) {
	if (data.size() != 4)
		throw dns_error(rcodes::format_error, "wrong A record size");
	addr_dword = *reinterpret_cast<const dword_t *>(data.data());
}

[[noreturn]] void ipaddr_format(const YAML::Node & node) {
	throw zone_error(node.Mark(), "wrong ipv4 address format");
}

std::uint8_t read_octet(std::istream & ss, const YAML::Node & node) {
	int c = ss.peek();
	if (!std::isdigit(c))
		ipaddr_format(node);
	unsigned octet;
	ss >> octet;
	if (octet > 255u)
		ipaddr_format(node);
	return static_cast<std::uint8_t>(octet);
}

void A::read(const YAML::Node & node, const name & zone) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			const std::string & value = node.as<std::string>();
			std::stringstream ss(value);
			bytes[0] = read_octet(ss, node);
			for (int i = 1; i < 4; ++i) {
				int c = ss.get();
				if (c != '.')
					ipaddr_format(node);
				bytes[i] = read_octet(ss, node);
			}
			if (ss.good())
				ipaddr_format(node);
			break;
		}
		case YAML::NodeType::Map: {
			read(node["address"], zone);
			break;
		}
		default: throw zone_error(node.Mark(), "wrong YAML node type for A record");
	}
}

std::string A::data_to_string() const {
	std::string result = std::to_string(unsigned(bytes[0]));
	for (auto i = 1; i < 4; ++i) {
		result += '.';
		result += std::to_string(unsigned(bytes[i]));
	}
	return result;
}

} // ktlo::dns::records
