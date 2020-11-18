#ifndef DNS_RECORDS_SRV_HEAD_TRHEDFTVR
#define DNS_RECORDS_SRV_HEAD_TRHEDFTVR

#include "record.hpp"

namespace ktlo::dns::records {

dns_record(SRV, 33) {
	word_t priority;
	word_t weight;
	word_t port;
	name target;

	virtual void encode(writer & wr) const override;
	virtual void decode(reader & rd) override;
	virtual void read(const YAML::Node & node, const name & hint) override;
	virtual std::string data_to_string() const override;
};

} // ktlo::dns::records

#endif // DNS_RECORDS_SRV_HEAD_TRHEDFTVR
