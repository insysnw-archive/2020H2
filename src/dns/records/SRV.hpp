#ifndef DNS_RECORDS_SRV_HEAD_TRHEDFTVR
#define DNS_RECORDS_SRV_HEAD_TRHEDFTVR

#include "record.hpp"

namespace ktlo::dns::records {

dns_record(SRV, 33) {
	word_t priority;
	word_t weight;
	word_t port;
	name target = gloabl_names.root();

	virtual void encode(varbytes & data) const override;
	virtual void decode(const varbytes_view & data) override;
	virtual void read(const YAML::Node & node, const name & zone) override;
	virtual std::string data_to_string() const override;
};

} // ktlo::dns::records

#endif // DNS_RECORDS_SRV_HEAD_TRHEDFTVR
