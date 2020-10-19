#include "record.hpp"

#include "question.hpp"
#include "zones.hpp"
#include "base64.hpp"

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

bool record::shoud_answer(const question & q) const {
	return q.qtype == 255 || q.qtype == type();
}

std::string record::to_string() const {
	return std::to_string(ttl) + '\t' + class_to_string(rclass) + '\t' + record::tname(type()) + '\t' + data_to_string();
}

// strange way to create `switch` statement
template <record_tids tid>
std::unique_ptr<record> record_create_bridge(record_tids id, record_classes rclass, std::uint32_t ttl) {
	if (id == tid)
		return record::create<tid>(rclass, ttl);
	else
		return record_create_bridge<static_cast<record_tids>(static_cast<std::int8_t>(tid) - 1)>(id, rclass, ttl);
}

template <>
inline std::unique_ptr<record> record_create_bridge<records::unknown::tid>(record_tids id, record_classes rclass, std::uint32_t ttl) {
	auto ptr = std::make_unique<records::unknown>(id);
	ptr->rclass = rclass;
	ptr->ttl = ttl;
	return ptr;
}

template <record_tids tid>
std::unique_ptr<record> record_create_bridge(const std::string_view & tname, record_classes rclass, std::uint32_t ttl) {
	if (records::by<tid>::tname == tname)
		return record::create<tid>(rclass, ttl);
	else
		return record_create_bridge<static_cast<record_tids>(static_cast<std::int8_t>(tid) - 1)>(tname, rclass, ttl);
}

template <>
inline std::unique_ptr<record> record_create_bridge<records::unknown::tid>(const std::string_view &, record_classes rclass, std::uint32_t ttl) {
	return record::create<records::unknown::tid>(rclass, ttl);
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

std::unique_ptr<record> record::create(record_tids tid, record_classes rclass, std::uint32_t ttl) {
	if (tid > max_tid)
		return record::create<records::unknown::tid>(rclass, ttl); // shortcut
	else
		return record_create_bridge<max_tid>(tid, rclass, ttl);
}

std::unique_ptr<record> record::create(const std::string_view & tname, record_classes rclass, std::uint32_t ttl) {
	return record_create_bridge<max_tid>(tname, rclass, ttl);
}

const char * record::tname(record_tids tid) {
	if (tid > max_tid)
		return records::by<records::unknown::tid>::tname;
	else
		return record_tname_bridge<max_tid>(tid);
}

std::vector<question> record::ask(const question &) const {
	return {};
}

namespace records {

void unknown::encode(varbytes & data) const {
	data = buffer;
}

void unknown::decode(const varbytes_view & data) {
	buffer = data;
}

void unknown::read(const YAML::Node & node, const name &) {
	throw zone_error(node.Mark(), "not implemented");
}

std::string unknown::data_to_string() const {
	return base64(buffer);
}

} // namespace records

} // ktlo::dns
