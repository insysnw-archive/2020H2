#include "namez.hpp"

#include <limits>
#include <algorithm>

namespace ktlo::dns {

std::shared_ptr<namez::node> namez::nodeof(const std::string_view & label, const std::shared_ptr<node> & parent) {
	auto & pchildren = parent->children;
	auto iter = std::find_if(pchildren.begin(), pchildren.end(), [&](const std::weak_ptr<node> & it) {
		std::shared_ptr ptr = it.lock();
		return ptr && ptr->label == label;
	});
	if (iter == pchildren.end()) {
		std::shared_ptr<node> result = std::make_shared<namez::node>(label, parent);
		pchildren.emplace_front(result);
		return result;
	} else {
		return iter->lock();
	}
}

std::shared_ptr<namez::node> namez::place(const std::string_view & string, const std::shared_ptr<node> & parent) {
	std::size_t i = string.find('.');
	if (i == std::numeric_limits<std::size_t>::max()) {
		return nodeof(string, parent);
	} else {
		auto new_parent = place(string.substr(i + 1), parent);
		return nodeof(string.substr(0, i), new_parent);
	}
}

void namez::cleanup(node & next) {
	next.children.remove_if([&](const std::weak_ptr<node> & child) -> bool {
		std::shared_ptr ptr = child.lock();
		if (ptr) {
			cleanup(*ptr);
			return false;
		} else {
			return true;
		}
	});
}

void namez::cleanup() {
	cleanup(*_root);
}

namez::namez() {
	_root = std::make_shared<node>();
}

name namez::add(const std::string_view & string) {
	if (string.size() > 0 && string[string.length() - 1] == '.') {
		auto view = string;
		view.remove_suffix(1);
		return add(view);
	} else {
		std::shared_ptr<node> top = place(string, _root);
		return name(top);
	}
}

name namez::add(const std::string_view & string, const name & suffix) {
	if (string.size() > 0 && string[string.length() - 1] == '.') {
		auto view = string;
		view.remove_suffix(1);
		return add(view, suffix);
	} else {
		std::shared_ptr<node> top = place(string, suffix.top);
		return name(top);
	}
}

name namez::resolve(const std::string_view & value, const name & zone) {
	if (value == "@" || value.empty()) {
		return zone;
	} else if (value[value.length() - 1] == '.') {
		return add(value);
	} else {
		return add(value, zone);
	}
}

name namez::root() const {
	return name(_root);
}

const std::string & name::domain() const {
	if (fullname.has_value())
		return fullname.value();
	if (is_root())
		return fullname.emplace(".");
	std::shared_ptr ptr = top;
	std::string & result = fullname.emplace(ptr->label);
	for (;;) {
		ptr = ptr->parent;
		if (!ptr)
			break;
		result += '.';
		result.append(ptr->label);
	}
	return result;
}

bool name::inside(const name & other) const {
	name current = *this;
	for (;!current.is_root(); current = current.parent()) {
		if (current == other)
			return true;
	}
	return current == other;
}

} // ktlo::dns
