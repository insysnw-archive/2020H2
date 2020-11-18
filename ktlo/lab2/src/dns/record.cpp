#include "record.hpp"

#include <ekutils/log.hpp>

#include "question.hpp"
#include "zones.hpp"
#include "base64.hpp"
#include "dnscodec.hpp"

#include "records/A.hpp"
#include "records/NS.hpp"
#include "records/CNAME.hpp"
#include "records/SOA.hpp"
#include "records/PTR.hpp"
#include "records/MX.hpp"
#include "records/TXT.hpp"
#include "records/AAAA.hpp"
#include "records/SRV.hpp"
#include "records/OPT.hpp"

namespace ktlo::dns {

thread_local ekutils::pocket<const zone> record_context_pocket;

record::record() :
	context(record_context_pocket),
	rclass(context.settings.rclass), ttl(context.settings.ttl)
{}

bool record::shoud_answer(const question & q) const {
	return q.qtype == 255 || q.qtype == type();
}

std::string record::to_string() const {
	return std::to_string(ttl) + '\t' + class_to_string(rclass) + '\t' + record::tname(type()) + '\t' + data_to_string();
}

// strange way to create `switch` statement
template <record_tids tid>
std::unique_ptr<record> record_create_bridge(record_tids id, const zone & context) {
	if (id == tid)
		return record::create<tid>(context);
	else
		return record_create_bridge<static_cast<record_tids>(static_cast<std::int8_t>(tid) - 1)>(id, context);
}

template <>
inline std::unique_ptr<record> record_create_bridge<records::unknown::tid>(record_tids id, const zone & context) {
	auto lock = record_context_pocket.use(context);
	return std::make_unique<records::unknown>(id);
}

template <record_tids tid>
std::unique_ptr<record> record_create_bridge(const std::string_view & tname, const zone & context) {
	if (records::by<tid>::tname == tname)
		return record::create<tid>(context);
	else
		return record_create_bridge<static_cast<record_tids>(static_cast<std::int8_t>(tid) - 1)>(tname, context);
}

template <>
inline std::unique_ptr<record> record_create_bridge<records::unknown::tid>(const std::string_view &, const zone & context) {
	return record::create<records::unknown::tid>(context);
}

template <record_tids tid>
constexpr const char * record_tname_bridge(record_tids id) {
	if (id == tid)
		return records::by<tid>::tname;
	else
		return record_tname_bridge<static_cast<record_tids>(static_cast<std::int8_t>(tid) - 1)>(id);
}

template <>
constexpr const char * record_tname_bridge<records::unknown::tid>(record_tids) {
	return records::by<records::unknown::tid>::tname;
}

constexpr record_tids max_tid = records::OPT::tid;

std::unique_ptr<record> record::create(record_tids tid, const zone & context) {
	if (tid > max_tid)
		return record::create<records::unknown::tid>(context); // shortcut
	else
		return record_create_bridge<max_tid>(tid, context);
}

std::unique_ptr<record> record::create(const std::string_view & tname, const zone & context) {
	return record_create_bridge<max_tid>(tname, context);
}

const char * record::tname(record_tids tid) {
	if (tid > max_tid)
		return records::by<records::unknown::tid>::tname;
	else
		return record_tname_bridge<max_tid>(tid);
}

std::vector<question_info> record::ask(const question_info &) const {
	return {};
}

namespace records {

void unknown::encode(writer & wr) const {
	wr.write_bytes(buffer);
}

void unknown::decode(reader & rd) {
	buffer = rd.read_all();
}

void unknown::read(const YAML::Node &, const name &) {
	throw std::invalid_argument("not implemented");
}

std::string unknown::data_to_string() const {
	return base64(buffer);
}

} // namespace records

} // ktlo::dns
