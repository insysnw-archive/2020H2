#ifndef DNS_RECORDS_SOA_HEAD_TRHEDFTVR
#define DNS_RECORDS_SOA_HEAD_TRHEDFTVR

#include "record.hpp"

namespace ktlo::dns::records {

dns_record(SOA, 6) {
	name mname;
	name rname;
	dword_t serial = 0u;
	dword_t refresh = 0u;
	dword_t retry = 0u;
	dword_t expire = 0u;
	dword_t minimum = 0u;

	virtual void encode(writer & wr) const override;
	virtual void decode(reader & rd) override;
	virtual void read(const YAML::Node & node, const name & hint) override;
	virtual std::string data_to_string() const override;
};

} // ktlo::dns::records

#endif // DNS_RECORDS_SOA_HEAD_TRHEDFTVR
