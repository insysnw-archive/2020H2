#ifndef DNS_RECORD_HEAD_PGEDFRGJUWQSAWD
#define DNS_RECORD_HEAD_PGEDFRGJUWQSAWD

#include <string>
#include <cinttypes>
#include <memory>
#include <vector>

#include "dns_enum.hpp"
#include "namez.hpp"
#include "dns_error.hpp"
#include "types.hpp"

namespace YAML {
	class Node;
} // namespace YAML

namespace ktlo::dns {

struct question;

namespace records {
	struct unknown;

	template <record_tids tid>
	struct by_s {
		typedef records::unknown record_type;
	};

	template <record_tids tid>
	using by = typename by_s<tid>::record_type;
} // namespace records

struct record {
	record_classes rclass;
	std::uint32_t ttl;

	virtual record_tids type() const = 0;
	virtual void encode(varbytes & data) const = 0;
	virtual void decode(const varbytes_view & data) = 0;
	virtual bool shoud_answer(const question & q) const;
	virtual std::vector<question> ask(const question & q) const;
	virtual void read(const YAML::Node & node, const name & zone) = 0;
	std::string to_string() const;

protected:
	virtual std::string data_to_string() const = 0;
	constexpr record() : rclass(record_classes::unknown), ttl(0u) {};

public:
	virtual ~record() {};

	template <record_tids tid>
	static std::unique_ptr<record> create(record_classes rclass, std::uint32_t ttl) {
		auto ptr = std::make_unique<records::by<tid>>();
		ptr->rclass = rclass;
		ptr->ttl = ttl;
		return ptr;
	}

	static std::unique_ptr<record> create(record_tids tid, record_classes rclass, std::uint32_t ttl);
	static std::unique_ptr<record> create(const std::string_view & tname, record_classes rclass, std::uint32_t ttl);
	static const char * tname(record_tids tid);
};

template <record_tids id, const char * type_name>
struct record_base : public record {
	static constexpr record_tids tid = id;
	static constexpr const char * tname = type_name;

	virtual record_tids type() const override final {
		return id;
	}
};

#define dns_record(record_type_name, tid) \
	struct record_type_name; \
	template <> \
	struct by_s<tid> { \
		typedef record_type_name record_type; \
	}; \
	constexpr const char name_##record_type_name[] = #record_type_name; \
	struct record_type_name final : public record_base<tid, name_##record_type_name>

namespace records {
	struct unknown final : public record {
		static constexpr const char * tname = "unknown";
		static constexpr record_tids tid = 0u;

		const record_tids stored;
		varbytes buffer;

		unknown() : stored(0u) {}
		unknown(record_tids t) : stored(t) {}

		virtual record_tids type() const override {
			return stored;
		}

		virtual void encode(varbytes & data) const override;
		virtual void decode(const varbytes_view & data) override;
		virtual void read(const YAML::Node &, const name &) override;
		virtual std::string data_to_string() const override;
	};
} // namespace records

} // ktlo::dns

#endif // DNS_RECORD_HEAD_PGEDFRGJUWQSAWD
