#ifndef CLIENT_HEAD_SDDFHJUYCSCSAASA
#define CLIENT_HEAD_SDDFHJUYCSCSAASA

#include <random>

#include <ekutils/udp_d.hpp>

#include "packet.hpp"

namespace ktlo::dns {

class client final {
	packet out_packet;
	packet in_packet;
	ekutils::net::client_udp_socket_d socket;
	std::random_device rd;
	std::uniform_int_distribution<word_t> distrib;
	varbytes tosend;

	static void move_answers(answers_bag::record_list & receiver, answers_bag::record_list & source);
	static void move_answers(answers_bag & receiver, answers_bag & source);

	void prepare();
	void ask(const question & q);
	void encode();
	bool request(answers_bag & result, const ekutils::net::endpoint & address);
	void recursive_request(answers_bag & result, const std::string & host, const std::string & port);
	void recursive_request(answers_bag & result, const ekutils::net::endpoint & address);
	void recursion(answers_bag & answers, const std::vector<answer> & next_NS, const ekutils::net::endpoint & address);

public:
	client(namez & names) : out_packet(names), in_packet(names) {}

	void recursive_request(answers_bag & result, const ekutils::net::endpoint & address, const question & q) {
		prepare();
		ask(q);
		encode();
		recursive_request(result, address);
	}

	void recursive_request(answers_bag & result, const std::string & host, const std::string & port, const question & q) {
		prepare();
		ask(q);
		encode();
		recursive_request(result, host, port);
	}
};

} // namespace ktlo::dns

#endif // CLIENT_HEAD_SDDFHJUYCSCSAASA
