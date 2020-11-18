#include "zones.hpp"

#include <ekutils/log.hpp>
#include <ekutils/resolver.hpp>

namespace ktlo::dns {

zone_error::zone_error(const YAML::Mark & mark, const std::string & message) :
	std::runtime_error(message + " (at line: " + std::to_string(mark.line) + ", column: " + std::to_string(mark.column) + ")") {}

namespace yamltags {
#	define YAML_TAG "tag:yaml.org,2002:"
/*
	constexpr const char * yint = YAML_TAG "int";
	constexpr const char * yfloat = YAML_TAG "float";
	constexpr const char * ystr = YAML_TAG "str";
	constexpr const char * ybool = YAML_TAG "bool";
	constexpr const char * ynull = YAML_TAG "null";
	constexpr const char * yseq = YAML_TAG "seq";
*/
	constexpr const char * ymap = YAML_TAG "map";
#	undef YAML_TAG
}

void db_add_record(
	const std::string & tag, const YAML::Node & value,
	const name & domain, zone & _zone, const name & hint
) {
	std::string_view view(tag.data() + 1, tag.size() - 1);
	try {
		auto rr = record::create(view, _zone);
		if (rr->type() == records::unknown::tid) {
			std::string errstr = "unknown record type: ";
			errstr.append(view);
			throw zone_error(value.Mark(), errstr);
		}
		rr->read(value, hint);
		_zone.add(domain, std::move(rr));
	} catch (const zone_error &) {
		throw;
	} catch (const std::invalid_argument & e) {
		throw zone_error(value.Mark(), e.what());
	} catch (const std::runtime_error & e) {
		throw zone_error(value.Mark(), std::string("error: ") + typeid(e).name() + ": " + e.what());
	}
}

record_classes resolve_class(const YAML::Node & rclass) {
	try {
		auto code = rclass.as<std::uint16_t>();
		return record_classes(code);
	} catch (const YAML::Exception &) {
		const std::string & str = rclass.as<std::string>();
		if (str == "IN")
			return record_classes::IN;
		else if (str == "CS")
			return record_classes::CS;
		else if (str == "CH")
			return record_classes::CH;
		else if (str == "HS")
			return record_classes::HS;
		else
			throw zone_error(rclass.Mark(), "unknown class");
	}
}

class hold_settings final {
	zone & m_zone;
	zone_settings settings;
public:
	hold_settings(zone & z) : m_zone(z), settings(z.settings) {}
	~hold_settings() {
		m_zone.settings = settings;
	}
};

void operator<<(zone_settings & settings, const YAML::Node & node) {
	if (auto ttl = node["$ttl"]) {
		settings.ttl = ttl.as<std::uint32_t>();
	}
	if (auto rclass = node["$class"]) {
		settings.rclass = resolve_class(rclass);
	}
}

void add_resolved(zone::addresses_t & addresses, const YAML::Node & node) {
	switch (node.Type()) {
		case YAML::NodeType::Undefined:
			return;
		case YAML::NodeType::Null:
			addresses.clear();
			return;
		case YAML::NodeType::Sequence:
			for (const auto & item : node)
				add_resolved(addresses, item);
			return;
		case YAML::NodeType::Scalar:
			try {
				auto targets = ekutils::net::resolve(node.as<std::string>(), "domain", ekutils::net::protocols::udp);
				for (auto & target : targets) {
					switch (target.address->family()) {
						case ekutils::net::family_t::ipv4:
							addresses.emplace_back(dynamic_cast<ekutils::net::ipv4::endpoint &>(*target.address).address());
							break;
						case ekutils::net::family_t::ipv6:
							addresses.emplace_back(dynamic_cast<ekutils::net::ipv6::endpoint &>(*target.address).address());
							break;
						default: log_fatal("unreachable");
					}
				}
			} catch (const std::exception & e) {
				throw zone_error(node.Mark(), e.what());
			}
			return;
		default: throw zone_error(node.Mark(), "wrong YAML type for address");
	}
}

void add_forwarder(std::vector<ekutils::uri> & forwarders, const YAML::Node & node) {
	switch (node.Type()) {
		case YAML::NodeType::Undefined:
			return;
		case YAML::NodeType::Null:
			forwarders.clear();
			return;
		case YAML::NodeType::Sequence:
			for (const auto & item : node)
				add_forwarder(forwarders, item);
			return;
		case YAML::NodeType::Scalar:
			try {
				forwarders.emplace_back("udp://" + node.as<std::string>());
			} catch (const std::exception & e) {
				throw zone_error(node.Mark(), e.what());
			}
			return;
		default: throw zone_error(node.Mark(), "wrong YAML type for forwarder");
	}
}

void read_zone(zone & _zone, const YAML::Node & node) {
	if (node.IsNull())
		return;
	if (!node.IsMap())
		throw zone_error(node.Mark(), "not a map");
	_zone.settings << node;

	if (auto forward = node["$forward"])
		add_forwarder(_zone.forward, forward);
	if (auto allow = node["$allow"])
		add_resolved(_zone.allowed, allow);
	if (auto deny = node["$deny"])
		add_resolved(_zone.denied, deny);

	for (const auto & subnode : node) {
		const YAML::Node & first = subnode.first;
		if (first.IsScalar() || first.IsNull()) {
			const std::string & key = first.IsNull() ? "" : first.as<std::string>();
			if (key.empty() || key[0] != '$') {
				const YAML::Node & value = subnode.second;
				const std::string & tag = value.Tag();
				name subdomain = _zone.names().resolve(key, _zone.domain);
				zone & actual_zone = key[key.size() - 1] == '.' ? _zone.db.zoneof(subdomain) : _zone;
				if (tag[0] == '!') {
					// records
					switch (value.Type()) {
						case YAML::NodeType::Scalar: {
							db_add_record(tag, value, subdomain, actual_zone, _zone.domain);
							break;
						}
						case YAML::NodeType::Sequence: {
							for (const auto & item : value)
								db_add_record(tag, item, subdomain, actual_zone, _zone.domain);
							break;
						}
						case YAML::NodeType::Map: {
							hold_settings hold_it(actual_zone);
							actual_zone.settings << value;
							db_add_record(tag, value, subdomain, actual_zone, _zone.domain);
							break;
						}
						default: break;
					} 
				} else if (tag == yamltags::ymap || tag == "?" || tag.empty()) {
					// subzone
					zone & subzone = actual_zone.declare(subdomain);
					read_zone(subzone, value);
				} else {
					log_warning("unknown YAML tag: " + tag);
				}
			}
		}
	}
}

void read(database & db, const YAML::Node & node) {
	zone & root = db.declare(db.names.root());
	read_zone(root, node);
}

} // ktlo::dns
