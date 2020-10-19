#ifndef NTP_ENUMS_HEAD_JRTGWESCEDC
#define NTP_ENUMS_HEAD_JRTGWESCEDC

#include <array>
#include <chrono>

#include "codec.hpp"

namespace ktlo {
	struct reader;
	struct writer;
} // namespace ktlo

namespace ktlo::ntp {

enum class leaps : byte_t {
	no_warning, sec61, sec59, unknown
};

enum class modes : byte_t {
	reserved, symmetric_active, symmetric_passive, client,
	server, broadcast, control, private_reserved
};

constexpr qword_t NTP_TIMESTAMP_DELTA = 2208988800ull;

template <typename int_t>
struct ntp_time {
	int_t seconds;
	int_t fraction;

	ntp_time<int_t> div2() const {
		int_t bit = seconds & 1;
		ntp_time result;
		result.seconds = seconds >> 1;
		result.fraction = (fraction >> 1) | (bit << (sizeof(int_t) * 8 - 1));
		return result;
	}

	operator std::time_t() const {
		return std::time_t(seconds - NTP_TIMESTAMP_DELTA);
	}

	std::string to_string() const {
		auto time = std::time_t(*this);
		return std::ctime(&time);
	}

	bool operator==(const ntp_time & other) const {
		return seconds == other.seconds && fraction == other.fraction;
	}

	bool operator!=(const ntp_time & other) const {
		return seconds != other.seconds || fraction != other.fraction;
	}
};

template <typename int_t>
std::ostream & operator<<(std::ostream & output, const ntp_time<int_t> & time) {
	return output << time.to_string();
}

template <typename int_t, typename R, typename P>
ntp_time<int_t> to_ntp_time(const std::chrono::duration<R, P> & duration) {
	using namespace std::chrono;
	seconds secs = duration_cast<seconds>(duration);
	ntp_time<int_t> result;
	result.seconds = secs.count();
	result.fraction = (secs * 0x1FFFFFFFFull).count();
	return result;
}

template <typename int_a, typename int_b>
ntp_time<int_a> operator+(const ntp_time<int_a> & a, const ntp_time<int_b> & b) {
	ntp_time<int_a> result;
	result.seconds = a.seconds + b.seconds;
	result.fraction = a.fraction + b.fraction;
	if (result.fraction < a.fraction)
		result.seconds++;
	return result;
}

template <typename int_a, typename int_b>
ntp_time<int_a> operator-(const ntp_time<int_a> & a, const ntp_time<int_b> & b) {
	ntp_time<int_a> result;
	result.seconds = a.seconds - b.seconds;
	result.fraction = a.fraction - b.fraction;
	if (result.fraction > a.fraction)
		result.seconds--;
	return result;
}

template <typename int_t, typename R, typename P>
ntp_time<int_t> operator+(const ntp_time<int_t> & time, const std::chrono::duration<R, P> & duration) {
	return time + to_ntp_time<int_t>(duration);
}

template <typename int_t, typename R, typename P>
ntp_time<int_t> operator-(const ntp_time<int_t> & time, const std::chrono::duration<R, P> & duration) {
	return time - to_ntp_time<int_t>(duration);
}

template <typename int_t>
reader & operator>>(reader & rd, ntp_time<int_t> & time) {
	time.seconds = rd.read<int_t>();
	time.fraction = rd.read<int_t>();
	return rd;
}

template <typename int_t>
writer & operator<<(writer & wr, const ntp_time<int_t> & time) {
	wr.write<int_t>(time.seconds);
	wr.write<int_t>(time.fraction);
	return wr;
}

typedef ntp_time<word_t> ntp_short;
typedef ntp_time<dword_t> ntp_timestamp;

template <>
struct ntp_time<qword_t> {
	qword_t seconds;
	qword_t fraction;

	dword_t era_number() const noexcept {
		return seconds >> 32;
	}

	dword_t era_offset() const noexcept {
		return seconds;
	}
};

typedef ntp_time<qword_t> ntp_date;

} // namespace ktlo::ntp

#endif // NTP_ENUMS_HEAD_JRTGWESCEDC
