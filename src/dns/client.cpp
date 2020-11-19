#include "client.hpp"

#include <ekutils/resolver.hpp>
#include <ekutils/udp_d.hpp>
#include <ekutils/log.hpp>
#include <ekutils/finally.hpp>

#include "records/A.hpp"
#include "records/AAAA.hpp"
#include "records/NS.hpp"

namespace ktlo::dns {

void client::move_answers(answers_bag::record_list & receiver, answers_bag::record_list & source) {
	receiver.reserve(receiver.size() + source.size());
	receiver.insert(receiver.end(), std::make_move_iterator(source.begin()), std::make_move_iterator(source.end()));
	source.clear();
}

void client::move_answers(answers_bag & receiver, answers_bag & source) {
	move_answers(receiver.answers, source.answers);
	move_answers(receiver.authority, source.authority);
	move_answers(receiver.additional, source.additional);
}

void client::prepare() {
	out_packet.answers.clear();
	out_packet.questions.clear();
	out_packet.head.id = distrib(rd);
	out_packet.head.recursion_desired = true;
	out_packet.head.recursion_available = false;
	out_packet.head.is_response = false;
	out_packet.head.opcode = opcodes::QUERY;
	out_packet.head.authoritative = false;
	out_packet.head.Z = 0;
	out_packet.head.trancated = false;
	out_packet.head.rcode = rcodes::no_error;
}

void client::ask(const question & q) {
	out_packet.questions.push_back(q);
}

void client::encode() {
	log_debug("ask other: " + out_packet.to_string());
	out_packet.write(tosend);
}

bool client::request(answers_bag & result, const ekutils::net::endpoint & address) {
	socket.open(address.family());
	socket.recv_timeout(std::chrono::seconds(3));
	socket.write(tosend.data(), tosend.size(), address);
	std::array<byte_t, 4000> torecv;
	socket.read(torecv.data(), torecv.size());
	in_packet.read(varbytes_view(torecv.data(), torecv.size()));
	log_debug("responsed other: " + in_packet.to_string());
	if (in_packet.head.rcode != rcodes::no_error)
		throw dns_error(in_packet.head.rcode, "forwarder responded with error");
	if (!in_packet.head.is_response)
		throw std::runtime_error("forwarder not set response flag");
	if (in_packet.head.id != out_packet.head.id)
		throw std::runtime_error("forwarder responded with different id");
	move_answers(result, in_packet.answers);
	if (!in_packet.head.authoritative && !in_packet.head.recursion_available) {
		// ask next
		log_debug("recursion not available, ask next");
		return false;
	}
	return true;
}

void client::recursion(answers_bag & answers, const std::vector<answer> & next_NS, const ekutils::net::endpoint & address) {
	word_t id_save = out_packet.head.id;
	varbytes data_save = std::move(tosend);
	finally({
		out_packet.head.id = id_save;
		tosend = data_save;
	});
	for (const answer & a : next_NS) {
		if (a.arecord().type() == records::NS::tid) {
			try {
				const records::NS & ns = dynamic_cast<const records::NS &>(a.arecord());
				question ns_question(ns.nsdname, records::A::tid, record_classes::IN);
				recursive_request(answers, address, ns_question);
				if (!answers.answers.empty()) {
					move_answers(answers.additional, answers.answers);
					return;
				}
			} catch (const std::exception & e) {
				log_warning(e);
			}
		}
	}
	throw std::runtime_error("no next name server address found");
}

void client::recursive_request(answers_bag & result, const ekutils::net::endpoint & address) {
	answers_bag answers;
	if (request(answers, address)) {
		move_answers(result, answers);
		return;
	}
	bool next_hop = true;
	std::size_t hop_count = 0;
	do {
		++hop_count;
		log_debug("forward next name server request #" + std::to_string(hop_count));
		std::vector<answer> next_NS = std::move(answers.authority);
		if (answers.additional.empty()) {
			log_debug("server didn't responded with next NS ip addresses, performing requests for NS...");
			answers.clear();
			recursion(answers, next_NS, address);
		}
		std::vector<answer> next_addresses = std::move(answers.additional);
		answers.clear();
		for (answer & a : next_addresses) {
			bool answered = false;
			try {
				const record & r = a.arecord();
				switch (r.type()) {
					case records::A::tid: {
						const records::A & a = dynamic_cast<const records::A &>(r);
						next_hop = !request(answers, ekutils::net::ipv4::endpoint(a.address, 53));
						answered = true;
						break;
					}
					case records::AAAA::tid: {
						const records::AAAA & aaaa = dynamic_cast<const records::AAAA &>(r);
						next_hop = !request(answers, ekutils::net::ipv6::endpoint(aaaa.address, 53));
						answered = true;
						break;
					}
					default: break;
				}
			} catch (const std::exception & e) {
				log_warning("forwarder #" + std::to_string(hop_count) + " error:");
				log_warning(e);
			}
			if (answered)
				break;
		}
	} while (next_hop);
	move_answers(result, answers);
}

void client::recursive_request(answers_bag & result, const std::string & host, const std::string & port) {
	auto targets = ekutils::net::resolve(host, port, ekutils::net::protocols::udp);
	if (targets.empty())
		throw std::runtime_error("no address associated with name: " + host + ':' + port);
	answers_bag answers;
	for (auto & target : targets) {
		try {
			recursive_request(result, *target.address);
			return;
		} catch (const std::exception & e) {
			log_warning(e);
		}
	}
	throw dns_error(rcodes::server_failure, "failed to gain response from: " + host + ':' + port);
}

} // namespace ktlo::dns
