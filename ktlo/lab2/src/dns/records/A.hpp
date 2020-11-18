#ifndef DNS_RECORDS_A_HEAD_TRHEDFTVR
#define DNS_RECORDS_A_HEAD_TRHEDFTVR

#include <ekutils/socket_d.hpp>

#include "record.hpp"

namespace ktlo::dns::records {

dns_record(A, 1) {
	ekutils::net::ipv4::address address;

	virtual void encode(writer & wr) const override;
	virtual void decode(reader & rd) override;
	virtual void read(const YAML::Node & node, const name & hint) override;
	virtual std::string data_to_string() const override;
};

} // ktlo::dns::records

#endif // DNS_RECORDS_A_HEAD_TRHEDFTVR
