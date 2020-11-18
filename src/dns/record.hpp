#ifndef DNS_RECORD_HEAD_PGEDFRGJUWQSAWD
#define DNS_RECORD_HEAD_PGEDFRGJUWQSAWD

#include <string>
#include <cinttypes>
#include <memory>
#include <vector>

#include <ekutils/pocket.hpp>

#include "dns_enum.hpp"
#include "namez.hpp"
#include "dns_error.hpp"
#include "types.hpp"
#include "question.hpp"

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

class database;

struct zone;

extern thread_local ekutils::pocket<const zone> record_context_pocket;

class reader;
class writer;

enum class answer_categories {
	regular, authority, additional
};

struct question_info {
	question q;
	answer_categories category;
};

struct record {
	const zone & context;

	record_classes rclass;
	std::uint32_t ttl;

	virtual record_tids type() const = 0;
	virtual void encode(writer & wr) const = 0;
	virtual void decode(reader & rd) = 0;
	virtual bool shoud_answer(const question & q) const;
	virtual std::vector<question_info> ask(const question_info & q) const;
	virtual void read(const YAML::Node & node, const name & hint) = 0;
	std::string to_string() const;

protected:
	virtual std::string data_to_string() const = 0;
	record();

public:
	virtual ~record() {};

	template <record_tids tid>
	static std::unique_ptr<record> create(const zone & context) {
		auto lock = record_context_pocket.use(context);
		return std::make_unique<records::by<tid>>();
	}

	static std::unique_ptr<record> create(record_tids tid, const zone & context);
	static std::unique_ptr<record> create(const std::string_view & tname, const zone & context);
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

		virtual void encode(writer & wr) const override;
		virtual void decode(reader & rd) override;
		virtual void read(const YAML::Node &, const name &) override;
		virtual std::string data_to_string() const override;
	};
} // namespace records

} // ktlo::dns

#endif // DNS_RECORD_HEAD_PGEDFRGJUWQSAWD
