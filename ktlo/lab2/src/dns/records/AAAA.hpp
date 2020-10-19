#ifndef DNS_RECORDS_AAAA_HEAD_TRHEDFTVR
#define DNS_RECORDS_AAAA_HEAD_TRHEDFTVR

#include "record.hpp"
#include "ipv6.hpp"

namespace ktlo::dns::records {

dns_record(AAAA, 28) {
	ipv6 address;

	virtual void encode(varbytes & data) const override;
	virtual void decode(const varbytes_view & data) override;
	virtual void read(const YAML::Node & node, const name & zone) override;
	virtual std::string data_to_string() const override;
};

} // ktlo::dns::records

#endif // DNS_RECORDS_AAAA_HEAD_TRHEDFTVR
