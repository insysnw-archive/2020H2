#ifndef DNS_ANSWER_HEAD_PBTRHGAREGW
#define DNS_ANSWER_HEAD_PBTRHGAREGW

#include <memory>
#include <variant>

#include "namez.hpp"
#include "record.hpp"

namespace ktlo::dns {

class answer {
	static const record & extract(const std::unique_ptr<const record> & r) {
		return *r;
	}
	static const record & extract(std::reference_wrapper<const record> r) {
		return r;
	}
	std::variant<std::unique_ptr<const record>, std::reference_wrapper<const record>> m_record;

public:
	name aname;
	const record & arecord() const {
		return std::visit([](const auto & arg) -> const record & { return extract(arg); }, m_record);
	}

	answer(const name & n, const record & r) : m_record(std::cref(r)), aname(n) {}
	answer(const name & n, std::unique_ptr<const record> && r) : m_record(std::move(r)), aname(n) {}

	std::string to_string() const;
};

struct answers_bag final {
	typedef std::vector<answer> record_list;

	record_list answers;
	record_list authority;
	record_list additional;

	void clear() {
		answers.clear();
		authority.clear();
		additional.clear();
	}
};

} // ktlo::dns

#endif // DNS_ANSWER_HEAD_PBTRHGAREGW
