#ifndef DNS_RECORDS_NS_HEAD_TRHEDFTVR
#define DNS_RECORDS_NS_HEAD_TRHEDFTVR

#include "record.hpp"

namespace ktlo::dns::records {

dns_record(NS, 2) {
	name nsdname;

	virtual void encode(writer & wr) const override;
	virtual void decode(reader & rd) override;
	virtual std::vector<question_info> ask(const question_info & q) const override;
	virtual void read(const YAML::Node & node, const name & hint) override;
	virtual std::string data_to_string() const override;
};

} // ktlo::dns::records

#endif // DNS_RECORDS_NS_HEAD_TRHEDFTVR
