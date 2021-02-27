#ifndef __SNMP_PROTOCOL_H__
#define __SNMP_PROTOCOL_H__

#include <sys/types.h>

#define DATA_TYPE_INT 2
#define DATA_TYPE_OCTET_STRING 4

struct snmp_header
{
    u_int32_t version : 24;
    u_int16_t pdu_type;
    char *community;
    u_int16_t community_len;
};

struct snmp_pdu
{
    u_int32_t id;
    u_int32_t error_status : 24;
    u_int32_t error_index : 24;
};

struct snmp_var_bind
{
    u_int16_t oid_length;
    u_int16_t oid_data_length;
    u_int8_t oid_data_type;
    char *oid;
    char *oid_data;
};

struct snmp_packet
{
    struct snmp_header header;
    struct snmp_pdu pdu;
    struct snmp_var_bind var_bind;
};

void snmp_print_oid(struct snmp_var_bind *var_bind);
void snmp_print_header(struct snmp_header *header);
void snmp_print_pdu(struct snmp_pdu *pdu);
void snmp_print_packet(struct snmp_packet *pkt);

int snmp_packet_pack(struct snmp_packet *pkt, void *packed);
int snmp_pdu_pack(struct snmp_pdu *pdu, void *packed);
int snmp_header_pack(struct snmp_header *header, void *packed);

int snmp_packet_parse(struct snmp_packet *pkt, void *data, u_int16_t size);

#endif