#ifndef DNS_SETTINGS_HEAD_QCPFKKKREG
#define DNS_SETTINGS_HEAD_QCPFKKKREG

#include <string>

#include <ekutils/log.hpp>

namespace YAML {
	class Node;
} // namespace YAML

namespace ktlo::dns {

struct settings {
	std::string address = "0.0.0.0";
	std::string port = "domain";
	ekutils::log_level verb = ekutils::log_level::info;
	bool dump = false;
	std::string forward = "";

	void apply(const YAML::Node & node);
};

} // ktlo::dns

#endif // DNS_SETTINGS_HEAD_QCPFKKKREG
