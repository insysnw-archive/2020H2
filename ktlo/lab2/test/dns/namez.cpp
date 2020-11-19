#include <iostream>

#include "namez.hpp"

#include "test.hpp"

test {
	using namespace ktlo::dns;

	namez ns;

	name zone = ns.add("handtruth.com");
	name mc = ns.add("mc", zone);
	name abcd = ns.add("a.b.c.d.example.com");
	ns.add("fantom");
	name mcgit = ns.add("mc.git", zone);

	std::array<std::string, 11> expectations {
		".",
		"com.",
		"example.com.",
		"d.example.com.",
		"c.d.example.com.",
		"b.c.d.example.com.",
		"a.b.c.d.example.com.",
		"handtruth.com.",
		"git.handtruth.com.",
		"mc.git.handtruth.com.",
		"mc.handtruth.com."
	};

	int i = 0;
	for (const auto & n : ns) {
		assert_equals(expectations.at(i++), n.domain());
	}
}
