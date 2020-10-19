#include "settings.hpp"

#include <yaml-cpp/yaml.h>

namespace ktlo::dns {

void settings::apply(const YAML::Node & node) {
	if (!node)
		return;
	if (!node.IsMap())
		throw std::runtime_error("$dns YAML node is not a map type");
	if (auto it = node["address"])
		address = it.as<std::string>();
	if (auto it = node["port"])
		port = it.as<std::string>();
	if (auto it = node["verb"]) {
		try {
			verb = ekutils::log_level(it.as<unsigned>());
		} catch (const YAML::Exception &) {
			verb = ekutils::str2loglvl(it.as<std::string>());
		}
	}
	if (auto it = node["dump"]) {
		dump = it.as<bool>();
	}
}

} // ktlo::dns
