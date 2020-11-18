#ifndef DNS_PACKET_HEAD_QPPKBGBTRPFVKG
#define DNS_PACKET_HEAD_QPPKBGBTRPFVKG

#include <cstring>

#include "packet.hpp"
#include "dns_error.hpp"
#include "dnscodec.hpp"

#include <iostream>
#include <bitset>

namespace ktlo::dns {

void packet::read(const varbytes_view & data) {
	answers.clear();
	if (data.size() < 12)
		throw dns_error(rcodes::format_error, "too small to be a DNS packet");
	reader rd(fake_db.names, data);
	head.id = rd.read<word_t>();
	std::bitset<16> flags = rd.read<word_t>();
	head.is_response = flags[15];
	head.opcode = opcodes((flags >> 11).to_ulong() & 0b1111);
	head.authoritative = flags[10];
	head.trancated = flags[9];
	head.recursion_desired = flags[8];
	head.recursion_available = flags[7];
	head.Z = static_cast<std::uint8_t>((flags >> 4).to_ulong() & 0b111);
	head.rcode = rcodes(flags.to_ulong() & 0xF);

	word_t qdcount = rd.read<word_t>();
	word_t ancount = rd.read<word_t>();
	word_t nscount = rd.read<word_t>();
	word_t arcount = rd.read<word_t>();

	// read questions
	questions.clear();
	questions.reserve(qdcount);
	for (word_t i = 0; i < qdcount; ++i) {
		name qname = rd.read_name();
		record_tids qtype = record_tids(rd.read<word_t>());
		record_classes qclass = record_classes(rd.read<word_t>());
		questions.emplace_back(qname, qtype, qclass);
	}

	// read answers
	read_records(rd, answers.answers, ancount);

	// read name server authority records
	read_records(rd, answers.authority, nscount);

	// read additional records
	read_records(rd, answers.additional, arcount);
}

void packet::write(varbytes & data) const {
	data.clear();
	writer wr(data);
	wr.write<word_t>(head.id);
	std::bitset<16> flags;
	flags[15] = head.is_response;
	flags |= (word_t(head.opcode) & 0xF) << 11;
	flags[10] = head.authoritative;
	flags[9] = head.trancated;
	flags[8] = head.recursion_desired;
	flags[7] = head.recursion_available;
	flags |= (head.Z & 0b111) << 4;
	flags |= word_t(head.rcode) & 0xF;
	wr.write<word_t>(static_cast<word_t>(flags.to_ulong()));
	wr.write<word_t>(questions.size());
	wr.write<word_t>(answers.answers.size());
	wr.write<word_t>(answers.authority.size());
	wr.write<word_t>(answers.additional.size());

	// write questions
	for (const question & q : questions) {
		wr.write_name(q.qname);
		wr.write<word_t>(word_t(q.qtype));
		wr.write<word_t>(word_t(q.qclass));
	}

	// write answers
	write_records(wr, answers.answers);

	// write name server authority records
	write_records(wr, answers.authority);

	// read additional records
	write_records(wr, answers.additional);
}

std::string packet::to_string() const {
	std::string result = "HEAD("
		"\n\tid: #" + std::to_string(head.id) +
		",\n\tis_response: " + (head.is_response ? "true" : "false") +
		",\n\topcode: " + opcode_to_string(head.opcode) +
		",\n\tauthoritative: " + (head.authoritative ? "true" : "false") +
		",\n\ttrancated: " + (head.trancated ? "true" : "false") +
		",\n\trecursion desired: " + (head.recursion_desired ? "true" : "false") +
		",\n\trecursion available: " + (head.recursion_available ? "true" : "false") +
		",\n\trcode: " + rcode_to_string(head.rcode) +
	"\n)\n";
	if (!questions.empty()) {
		result += "QUESTIONS(";
		for (const question & q : questions) {
			result += "\n\t" + q.to_string();
		}
		result += "\n)\n";
	}
	if (!answers.answers.empty()) {
		result += "ANSWERS(";
		for (const answer & a : answers.answers) {
			result += "\n\t" + a.to_string();
		}
		result += "\n)\n";
	}
	if (!answers.authority.empty()) {
		result += "AUTHORITY(";
		for (const answer & a : answers.authority) {
			result += "\n\t" + a.to_string();
		}
		result += "\n)\n";
	}
	if (!answers.additional.empty()) {
		result += "ADDITIONAL(";
		for (const answer & a : answers.additional) {
			result += "\n\t" + a.to_string();
		}
		result += "\n)\n";
	}
	return result;
}

void packet::read_records(reader & rd, record_list & list, word_t count) {
	list.reserve(count);
	zone & context = fake_db.root();
	for (word_t i = 0; i < count; ++i) {
		name aname = rd.read_name();
		record_tids rtype = record_tids(rd.read<word_t>());
		record_classes rclass = record_classes(rd.read<word_t>());
		dword_t ttl = rd.read<dword_t>();
		word_t rsize = rd.read<word_t>();
		reader rdata = rd.record_reader(rsize);
		context.settings = { ttl, rclass };
		auto ptr = record::create(rtype, context);
		ptr->decode(rdata);
		list.emplace_back(answer(aname, std::move(ptr)));
	}
}

void packet::write_records(writer & wr, const record_list & list) const {
	for (const answer & item : list) {
		wr.write_name(item.aname);
		const record & r = item.arecord();
		wr.write<word_t>(word_t(r.type()));
		wr.write<word_t>(word_t(r.rclass));
		wr.write<dword_t>(r.ttl);
		word_t & size = wr.write_raw<word_t>();
		writer rdata = wr.record_writer();
		r.encode(rdata);
		std::size_t rsize = rdata.size();
		assert(rsize < std::numeric_limits<word_t>::max());
		size = hton<word_t>(rsize);
	}
}

} // ktlo::dns

#endif // DNS_PACKET_HEAD_QPPKBGBTRPFVKG
