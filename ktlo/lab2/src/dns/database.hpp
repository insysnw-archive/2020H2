#ifndef DNS_DATABASE_HEAD_WPVKRVRVKDE
#define DNS_DATABASE_HEAD_WPVKRVRVKDE

#include <map>
#include <memory>
#include <optional>
#include <forward_list>

#include <ekutils/uri.hpp>
#include <ekutils/socket_d.hpp>

#include "namez.hpp"
#include "record.hpp"
#include "question.hpp"
#include "answer.hpp"

namespace ktlo::dns {

class database;

struct zone_settings final {
	std::uint32_t ttl = 14400u;
	record_classes rclass = record_classes::IN;
};

struct zone final {
	database & db;
	name domain;
	zone_settings settings;
	std::vector<ekutils::uri> forward;
	typedef std::vector<std::variant<ekutils::net::ipv4::address, ekutils::net::ipv6::address>> addresses_t;
	addresses_t allowed;
	addresses_t denied;
	std::multimap<name, std::unique_ptr<const record>> records;

	zone(database & zdb, name n) : db(zdb), domain(n) {}

	const record & add(const name & n, std::unique_ptr<const record> && rr);

	namez & names() const;

	zone & declare(const name & other) const;

	void ask(answers_bag & bag, const question_info & q);
	bool is_authoritative();
};

struct database final {
	namez & names;
	std::map<name, zone> zones;

	zone & zoneof(const name & n);
	zone & declare(const name & n);

	zone & root() {
		return declare(names.root());
	}

	explicit database(namez & ns) : names(ns) { root(); }

	std::string to_string() const;
};

inline namez & zone::names() const {
	return db.names;
}

inline zone & zone::declare(const name & other) const {
	zone & result = db.declare(other);
	result.settings = settings;
	return result;
}

} // ktlo::dns

#endif // DNS_DATABASE_HEAD_WPVKRVRVKDE
