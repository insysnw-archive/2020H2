#include "database.hpp"

#include <ekutils/log.hpp>

#include "packet.hpp"
#include "records/SOA.hpp"

namespace ktlo::dns {

const record & zone::add(const name & n, std::unique_ptr<const record> && rr) {
	if (!n.inside(domain))
		throw std::invalid_argument("name \"" + n.domain() + "\" is not in domain \"" + domain.domain() + "\"");
	return *records.emplace(n, std::move(rr))->second;
}

inline answers_bag::record_list & select_container(answers_bag & bag, answer_categories category) {
	switch (category) {
	case answer_categories::regular:
		return bag.answers;
	case answer_categories::authority:
		return bag.authority;
	case answer_categories::additional:
		return bag.additional;
	default:
		log_fatal("unknown answer category");
	}
}

void zone::ask(answers_bag & bag, const question_info & q) {
	auto iters = records.equal_range(q.q.qname);
	for (auto iter = iters.first; iter != iters.second; ++iter) {
		const record & rr = *iter->second;
		if (q.q.qclass == rr.rclass && rr.shoud_answer(q.q)) {
			select_container(bag, q.category).emplace_back(iter->first, rr);
			auto additional_questions = rr.ask(q);
			for (const auto & each : additional_questions) {
				ask(bag, each);
			}
		}
	}
}

bool zone::is_authoritative() {
	auto iters = records.equal_range(domain);
	for (auto iter = iters.first; iter != iters.second; ++iter) {
		const record & rr = *iter->second;
		if (rr.type() == records::SOA::tid)
			return true;
	}
	return false;
}

zone & database::zoneof(const name & n) {
	name current = n;
	for (;!current.is_root(); current = current.parent()) {
		auto found = zones.find(current);
		if (found != zones.end())
			return found->second;
	}
	auto found = zones.find(current);
	if (found != zones.end())
		return found->second;
	log_fatal("zone should always be found");
}

zone & database::declare(const name & n) {
	return zones.try_emplace(n, *this, n).first->second;
}

std::string database::to_string() const {
	std::string result;
	for (const auto & zit : zones) {
		result += "ZONE: ";
		result += zit.second.domain.domain() + '\n';
		for (const auto & rr : zit.second.records) {
			result += '\t';
			result += rr.first.domain();
			result += '\t';
			result += rr.second->to_string();
			result += '\n';
		}
	}
	return result;
}

} // ktlo::dns
