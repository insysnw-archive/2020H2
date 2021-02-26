#ifndef __DNS_PROTOCOL_H__
#define __DNS_PROTOCOL_H__

#include <sys/types.h>

#define QR_QUERY 0
#define QR_RESPONSE 1

#define OPCODE_QUERY 0	/* a standard query */
#define OPCODE_IQUERY 1 /* an inverse query */
#define OPCODE_STATUS 2 /* a server status request */

#define AA_NONAUTHORITY 0
#define AA_AUTHORITY 1

struct dns_header
{
	u_int16_t id;
	/*u_int16_t qr : 1;
	u_int16_t opcode : 4;
	u_int16_t aa : 1;
	u_int16_t tc : 1;
	u_int16_t rd : 1;
	u_int16_t ra : 1;
	u_int16_t z : 3;
	u_int16_t rcode : 4;*/
	u_int16_t flags;
	u_int16_t qdcount;
	u_int16_t ancount;
	u_int16_t nscount;
	u_int16_t arcount;
};

struct dns_question
{
	u_int16_t qtype;
	u_int16_t qclass;
	char *qname;
	u_int16_t qname_len;
};

struct dns_resource_record
{
	u_int16_t type;
	u_int16_t class;
	u_int32_t ttl;
	u_int16_t rdlength;
	char *name;
	char *rdata;
};

struct dns_packet
{
	struct dns_header header;
	struct dns_question question;
	struct dns_resource_record rr;
	char *data;
	u_int16_t data_size;
};

void dns_print_header(struct dns_header *header);
void dns_print_packet(struct dns_packet *packet);
void dns_print_rr(struct dns_resource_record *rr);
void dns_print_question(struct dns_question *q);

int dns_request_parse(struct dns_packet *pkt, void *data, u_int16_t size);
int dns_header_parse(struct dns_header *header, void *data);
int dns_question_parse(struct dns_question *q, void *data, u_int16_t size);
int dns_packet_pack(struct dns_packet *pkt, void *packed);

#endif