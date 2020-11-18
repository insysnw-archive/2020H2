#include "records/TXT.hpp"

#include <yaml-cpp/yaml.h>

#include "zones.hpp"
#include "dnscodec.hpp"

namespace ktlo::dns::records {

void TXT::encode(writer & wr) const {
	for (const std::string & text : texts) {
		wr.write<byte_t>(static_cast<byte_t>(text.size()));
		wr.write_bytes(varbytes_view(reinterpret_cast<const byte_t *>(text.data()), text.size()));
	}
}

void TXT::decode(reader & rd) {
	texts.clear();
	for (;;) {
		if (!rd.pending())
			return;
		std::size_t size = rd.read<byte_t>();
		std::string_view text(reinterpret_cast<const char *>(rd.read_bytes(size).data(), size));
		texts.emplace_back(text);
	}
}

void TXT::read(const YAML::Node & node, const name & hint) {
	switch (node.Type()) {
		case YAML::NodeType::Scalar: {
			texts.emplace_back(node.as<std::string>());
			break;
		}
		case YAML::NodeType::Sequence: {
			for (const YAML::Node & item : node)
				texts.emplace_back(item.as<std::string>());
			break;
		}
		case YAML::NodeType::Map: {
			read(node["text"], hint);
			break;
		}
		default: throw zone_error(node.Mark(), "wrong YAML node type for TXT record");
	}
}

std::string quote(const std::string & str) {
	YAML::Emitter emit;
	emit << YAML::DoubleQuoted << YAML::EscapeNonAscii << YAML::Flow << str;
	return emit.c_str();
}

std::string TXT::data_to_string() const {
	std::string result;
	for (const std::string & text : texts) {
		result += quote(text);
		result += ' ';
	}
	return result;
}

} // ktlo::dns::records
