#include "records/TXT.hpp"

#include <yaml-cpp/yaml.h>

#include "zones.hpp"

namespace ktlo::dns::records {

void TXT::encode(varbytes & data) const {
	for (const std::string & text : texts) {
		data += static_cast<byte_t>(text.size());
		data += varbytes_view(reinterpret_cast<const byte_t *>(text.data()), text.size());
	}
}

void TXT::decode(const varbytes_view & data) {
	texts.clear();
	std::size_t i = 0;
	for (;;) {
		if (i >= data.size())
			return;
		std::size_t size = data[i];
		std::string_view text(reinterpret_cast<const char *>(data.data() + i + 1), size);
		texts.emplace_back(text);
		i += size + 1;
	}
}

void TXT::read(const YAML::Node & node, const name & zone) {
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
			read(node["text"], zone);
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
