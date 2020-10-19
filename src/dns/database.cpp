#include "database.hpp"

namespace ktlo::dns {

const record & database::add(name n, std::unique_ptr<const record> && rr) {
	return *data.emplace(n, std::move(rr))->second;
}

void database::ask(std::vector<answer> & result, const std::vector<question> & questions) {
	for (const question & q : questions) {
		auto iters = data.equal_range(q.qname);
		for (auto iter = iters.first; iter != iters.second; ++iter) {
			const record & rr = *iter->second;
			if (q.qclass == rr.rclass && rr.shoud_answer(q)) {
				result.emplace_back(iter->first, rr);
				ask(result, rr.ask(q));
			}
		}
	}
}

std::string database::to_string() const {
	std::string result;
	for (const auto & it : data) {
		result += it.first.domain();
		result += "\t";
		result += it.second->to_string();
		result += '\n';
	}
	return result;
}

} // ktlo::dns
