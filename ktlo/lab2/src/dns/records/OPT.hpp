#ifndef DNS_RECORDS_OPT_HEAD_TRHERGASWE
#define DNS_RECORDS_OPT_HEAD_TRHERGASWE

#include "record.hpp"

namespace ktlo::dns::records {

dns_record(OPT, 41) {
	varbytes buffer;

	virtual void encode(varbytes & data) const override;
	virtual void decode(const varbytes_view & data) override;
	virtual void read(const YAML::Node & node, const name & zone) override;
	virtual std::string data_to_string() const override;
};

} // ktlo::dns::records

#endif // DNS_RECORDS_OPT_HEAD_TRHERGASWE
