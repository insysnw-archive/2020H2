#ifndef DNS_RECORDS_OPT_HEAD_TRHERGASWE
#define DNS_RECORDS_OPT_HEAD_TRHERGASWE

#include "record.hpp"

namespace ktlo::dns::records {

dns_record(OPT, 41) {
	varbytes buffer;

	virtual void encode(writer & wr) const override;
	virtual void decode(reader & rd) override;
	virtual void read(const YAML::Node & node, const name & hint) override;
	virtual std::string data_to_string() const override;
};

} // ktlo::dns::records

#endif // DNS_RECORDS_OPT_HEAD_TRHERGASWE
