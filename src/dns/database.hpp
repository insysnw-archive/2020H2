#ifndef DNS_DATABASE_HEAD_WPVKRVRVKDE
#define DNS_DATABASE_HEAD_WPVKRVRVKDE

#include <map>
#include <memory>
#include <optional>

#include "namez.hpp"
#include "record.hpp"
#include "question.hpp"
#include "answer.hpp"

namespace ktlo::dns {

class database final {
	std::multimap<name, std::unique_ptr<const record>> data;

public:
	const record & add(name n, std::unique_ptr<const record> && rr);

private:
	void ask(std::vector<answer> & result, const std::vector<question> & questions);
	std::optional<answer> authority(question & question);

public:
	std::vector<answer> ask(const std::vector<question> & questions) {
		std::vector<answer> result;
		ask(result, questions);
		return result;
	}

	const std::multimap<name, std::unique_ptr<const record>> & all() const noexcept {
		return data;
	}

	std::string to_string() const;
};

} // ktlo::dns

#endif // DNS_DATABASE_HEAD_WPVKRVRVKDE
