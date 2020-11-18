#ifndef DNS_NAMEZ_HEAD_PWLOCMGTQJQRKVPDSKJ
#define DNS_NAMEZ_HEAD_PWLOCMGTQJQRKVPDSKJ

#include <string>
#include <string_view>
#include <list>
#include <memory>
#include <cassert>
#include <optional>
#include <algorithm>

namespace ktlo::dns {

class name;

class namez final {
	struct node;
	typedef std::list<std::weak_ptr<node>> node_list;

	struct node {
		std::string label;
		std::shared_ptr<node> parent;
		node_list children;

		node() {}
		node(const std::string_view & str, const std::shared_ptr<node> & ptr) : label(str), parent(ptr) {}
	};

	std::shared_ptr<node> _root;

	std::shared_ptr<node> nodeof(const std::string_view & label, const std::shared_ptr<node> & parent);

	std::shared_ptr<node> place(const std::string_view & string, const std::shared_ptr<node> & parent);
	
	void cleanup(node & next);

public:
	namez();
	name add(const std::string_view & string);
	name add(const std::string_view & string, const name & suffix);
	name resolve(const std::string_view & value, const name & zone);
	name root() const;
	void cleanup();

	template <typename type>
	class iterator_template {
		std::shared_ptr<node> current;

		iterator_template(std::shared_ptr<node> curr) : current(curr) {}
	public:
		typedef type value_type;
		iterator_template(const iterator_template & other) : current(other.current) {}
		iterator_template & operator=(const iterator_template & other) {
			current = other.current;
			return *this;
		}
		iterator_template & operator++() {
			if (current->children.empty()) {
				auto ptr = current;
				for (;;) {
					auto parent = ptr->parent;
					if (!parent) {
						current.reset();
						break;
					}
					node_list & children = parent->children;
					auto iter = std::find_if(children.begin(), children.end(), [&](const std::weak_ptr<node> & it) {
						return it.lock().get() == ptr.get();
					});
					assert(iter != children.end());
					++iter;
					if (iter == children.end()) {
						ptr = parent;
					} else {
						current = iter->lock();
						break;
					}
				}
			} else {
				current = current->children.front().lock();
			}
			return *this;
		}
		value_type operator*() const {
			return value_type(current);
		}
		template <typename T>
		friend bool operator==(const iterator_template & a, const iterator_template<T> & b) {
			return a.current == b.current;
		}
		template <typename T>
		friend bool operator!=(const iterator_template & a, const iterator_template<T> & b) {
			return a.current != b.current;
		}
		friend namez;
	};

	typedef iterator_template<name> iterator;
	typedef iterator_template<const name> const_iterator;

	const_iterator cbegin() const {
		return const_iterator(_root);
	}
	const_iterator cend() const {
		return const_iterator(nullptr);
	}
	iterator begin() {
		return iterator(_root);
	}
	iterator end() {
		return iterator(nullptr);
	}
	const_iterator begin() const {
		return cbegin();
	}
	const_iterator end() const {
		return cend();
	}

friend class name;
};

class name final {
	std::shared_ptr<namez::node> top;
	mutable std::optional<std::string> fullname;

public:
	name() = default;
	explicit name(const std::shared_ptr<namez::node> & t) : top(t), fullname(std::nullopt) {}
	name(const name & other) = default;
	name(name && other) : top(std::move(other.top)), fullname(std::move(other.fullname)) {
		other.fullname.reset();
	}
	name & operator=(const name & other) = default;
	name & operator=(name && other) {
		top = std::move(other.top);
		fullname = std::move(other.fullname);
		other.fullname.reset();
		return *this;
	}
	const std::string & domain() const;
	const std::string & label() const {
		return top->label;
	}
	bool is_root() const {
		return !top->parent;
	}
	bool inside(const name & other) const;
	name parent() const {
		return name(top->parent);
	}
	bool operator==(const name & other) const noexcept {
		return top.get() == other.top.get();
	}
	bool operator!=(const name & other) const noexcept {
		return top.get() != other.top.get();
	}
	bool operator<(const name & other) const noexcept {
		return top.get() < other.top.get();
	}
	bool operator>(const name & other) const noexcept {
		return top.get() > other.top.get();
	}
	bool operator<=(const name & other) const noexcept {
		return top.get() <= other.top.get();
	}
	bool operator>=(const name & other) const noexcept {
		return top.get() >= other.top.get();
	}

friend class namez;
friend struct std::hash<ktlo::dns::name>;
};

} // ktlo::dns

namespace std {
	template<> struct hash<ktlo::dns::name> {
		std::size_t operator()(const ktlo::dns::name & s) const noexcept {
			return reinterpret_cast<std::size_t>(s.top.get());
		}
	};
} // namespace std

#endif // DNS_NAMEZ_HEAD_PWLOCMGTQJQRKVPDSKJ
