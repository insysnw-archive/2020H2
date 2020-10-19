#ifndef DNS_ZONES_HEAD_QDTRHYTJDCDC
#define DNS_ZONES_HEAD_QDTRHYTJDCDC

#include <stdexcept>

#include <yaml-cpp/yaml.h>

#include "database.hpp"

namespace ktlo::dns {

struct zone_error : public std::runtime_error {
	zone_error(const YAML::Mark & mark, const std::string & message);
};

database read(namez & ns, const YAML::Node & node);

} // ktlo::dns

#endif // DNS_ZONES_HEAD_QDTRHYTJDCDC
