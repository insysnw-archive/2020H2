#ifndef DNS_PACKET_HEAD_QPXPPCKFEK
#define DNS_PACKET_HEAD_QPXPPCKFEK

#include <cinttypes>
#include <vector>
#include <memory>

#include "database.hpp"
#include "record.hpp"
#include "question.hpp"
#include "answer.hpp"

namespace ktlo::dns {

class reader;
class writer;

class packet final {
	database fake_db;

public:
	packet(namez & names) : fake_db(names) {}

	struct header final {
		word_t id;
		bool is_response;
		opcodes opcode;
		bool authoritative;
		bool trancated;
		bool recursion_desired;
		bool recursion_available;
		byte_t Z;
		rcodes rcode;
	} head;

	typedef std::vector<answer> record_list;

	std::vector<question> questions;
	answers_bag answers;

	void read(const varbytes_view & data);
	void write(varbytes & data) const;

	std::string to_string() const;

private:
	void read_records(reader & rd, record_list & list, word_t count);
	void write_records(writer & wr, const record_list & list) const;
};

} // ktlo::dns

#endif // DNS_PACKET_HEAD_QPXPPCKFEK
