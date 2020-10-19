#include "zones.hpp"

#include <ekutils/log.hpp>

namespace ktlo::dns {

zone_error::zone_error(const YAML::Mark & mark, const std::string & message) :
	std::runtime_error(message + " (at line: " + std::to_string(mark.line) + ", column: " + std::to_string(mark.column) + ")") {}

struct zone_info {
	std::uint32_t ttl = 14400u;
	record_classes rclass = record_classes::IN;
};

struct zone_shared {
	namez & ns;
	database & db;
};

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
	database & db, const zone_info & info, const std::string & tag,
	const YAML::Node & value, const name & domain, const name & zone
) {
	std::string_view view(tag.data() + 1, tag.size() - 1);
	auto rr = record::create(view, info.rclass, info.ttl);
	if (rr->type() == records::unknown::tid) {
		std::string errstr = "unknown record type: ";
		errstr.append(view);
		throw zone_error(value.Mark(), errstr);
	}
	rr->read(value, zone);
	db.add(domain, std::move(rr));
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

void read_subzone(const zone_shared & shared, const zone_info & parent_info, const YAML::Node & node, const name & domain) {
	if (!node.IsMap())
		throw std::runtime_error("not a map");
	zone_info info = parent_info;
	if (auto ttl = node["$ttl"]) {
		info.ttl = ttl.as<std::uint32_t>();
	}
	if (auto rclass = node["$class"]) {
		info.rclass = resolve_class(rclass);
	}
	for (const auto & subnode : node) {
		const YAML::Node & first = subnode.first;
		if (first.IsScalar() || first.IsNull()) {
			const std::string & key = first.IsNull() ? "" : first.as<std::string>();
			if (key.empty() || key[0] != '$') {
				const YAML::Node & value = subnode.second;
				const std::string & tag = value.Tag();
				name subdomain = shared.ns.resolve(key, domain);
				if (tag[0] == '!') {
					// records
					switch (value.Type()) {
						case YAML::NodeType::Scalar: {
							db_add_record(shared.db, info, tag, value, subdomain, domain);
							break;
						}
						case YAML::NodeType::Sequence: {
							for (const auto & item : value)
								db_add_record(shared.db, info, tag, item, subdomain, domain);
							break;
						}
						case YAML::NodeType::Map: {
							zone_info subinfo = info;
							if (auto ttl = value["$ttl"]) {
								subinfo.ttl = ttl.as<std::uint32_t>();
							}
							if (auto rclass = value["$class"]) {
								subinfo.rclass = resolve_class(rclass);
							}
							db_add_record(shared.db, subinfo, tag, value, subdomain, domain);
							break;
						}
						default: break;
					} 
				} else if (tag == yamltags::ymap || tag == "?" || tag.empty()) {
					// subzone
					read_subzone(shared, info, value, subdomain);
				} else {
					log_warning("unknown YAML tag: " + tag);
				}
			}
		}
	}
}

database read(namez & ns, const YAML::Node & node) {
	database db;
	zone_shared shared { ns, db };
	zone_info info { };
	read_subzone(shared, info, node, ns.root());
	return db;
}

} // ktlo::dns
