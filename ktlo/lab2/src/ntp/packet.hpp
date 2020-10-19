#ifndef NTP_PACKET_HEAD_WPVFVBBTGSEV
#define NTP_PACKET_HEAD_WPVFVBBTGSEV

#include <cinttypes>
#include <array>
#include <vector>

#include "ntp_types.hpp"
#include "extension.hpp"

namespace ktlo::ntp {

struct packet final {
	leaps leap : 2;
	unsigned version : 3;
	modes mode : 3;
	unsigned stratum : 8;
	signed poll : 8;
	signed precision : 8;

	dword_t rootdelay;
	dword_t rootdisp;
	union {
		dword_t refcode;
		char refid[5];
	};

	ntp_timestamp reference;
	ntp_timestamp origin;
	ntp_timestamp receive;
	ntp_timestamp transmit;
	//ntp_timestamp destination;
	std::vector<extension> extensions;
	dword_t keyid;

	packet() : leap(leaps::no_warning), version(4u), mode(modes::client), poll(0), precision(0) {}

	void read(const varbytes_view & src);
	void write(varbytes & dest);
};

} // ktlo::ntp

#endif // NTP_PACKET_HEAD_WPVFVBBTGSEV
