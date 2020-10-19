#ifndef DNS_RECORDS_A_HEAD_TRHEDFTVR
#define DNS_RECORDS_A_HEAD_TRHEDFTVR

#include "record.hpp"

namespace ktlo::dns::records {

dns_record(A, 1) {
	union {
		dword_t addr_dword;
		std::array<std::uint8_t, 4> bytes;
	};

	virtual void encode(varbytes & data) const override;
	virtual void decode(const varbytes_view & data) override;
	virtual void read(const YAML::Node & node, const name & zone) override;
	virtual std::string data_to_string() const override;
};

} // ktlo::dns::records

#endif // DNS_RECORDS_A_HEAD_TRHEDFTVR
