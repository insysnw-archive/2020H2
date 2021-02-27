#include <arpa/inet.h>

#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <errno.h>
#include <string.h>

#include "snmp_protocol.h"

void snmp_print_oid(struct snmp_var_bind *var_bind)
{
    printf("OID Length: %d\n", var_bind->oid_length);
    printf("OID: %s\n", var_bind->oid);
    printf("OID data: %s\n", var_bind->oid_data);
}

void snmp_print_header(struct snmp_header *header)
{
    printf("Version: %d\n", header->version);
    printf("PDU_type: %d\n", header->pdu_type);
    printf("Community: %s\n", header->community);
}

void snmp_print_pdu(struct snmp_pdu *pdu)
{
    printf("id: %d\n", pdu->id);
    printf("Error status: %d\n", pdu->error_status);
    printf("Error index: %d\n", pdu->error_index);
}

void snmp_print_packet(struct snmp_packet *pkt)
{
    snmp_print_header(&pkt->header);
    snmp_print_pdu(&pkt->pdu);
    snmp_print_oid(&pkt->var_bind);
}

int snmp_packet_parse(struct snmp_packet *pkt, void *data, u_int16_t size)
{
    //pkt->header = calloc(1, sizeof(struct snmp_header));
    //pkt->pdu = calloc(1, sizeof(struct snmp_pdu));
    //pkt->var_bind = calloc(1, sizeof(struct snmp_var_bind));

    int processed = 0;
    if (*((char *)data) != 48)
    {
        return -1;
    }
    processed += 3;

    if (*((char *)data + processed) != 2)
    {
        return -1;
    }
    pkt->header.version = *((char *)data + processed + 2);
    processed += 3;

    if (*((char *)data + processed) != 4)
    {
        return -1;
    }
    pkt->header.community_len = *((char *)data + processed + 1);
    pkt->header.community = calloc(pkt->header.community_len, sizeof(char));
    memcpy(pkt->header.community, data + processed + 2, pkt->header.community_len);
    processed += 2 + pkt->header.community_len;

    char type = *((char *)data + processed);
    switch (type)
    {
    case 160:
        pkt->header.pdu_type = 0;
        break;
    case 161:
        pkt->header.pdu_type = 1;
        break;
    case 162:
        pkt->header.pdu_type = 2;
        break;
    case 163:
        pkt->header.pdu_type = 3;
        break;

    default:
        break;
    }
    processed += 3;

    if (*((char *)data + processed) != 2)
    {
        return -1;
    }
    memcpy(&pkt->pdu, data + processed + 2, *((char *)data + processed + 1));
    pkt->pdu.id = htons(pkt->pdu.id);
    processed += 2 + *((char *)data + processed + 1);

    if (*((char *)data + processed) != 2)
    {
        return -1;
    }
    pkt->pdu.error_status = *((char *)data + processed + 2);
    processed += 3;

    if (*((char *)data + processed) != 2)
    {
        return -1;
    }
    pkt->pdu.error_index = *((char *)data + processed + 2);
    processed += 3;

    if (*((char *)data + processed) != 48)
    {
        return -1;
    }
    processed += 3;

    if (*((char *)data + processed) != 48)
    {
        return -1;
    }
    pkt->var_bind.oid_length = *((char *)data + processed + 1);
    memcpy(pkt->var_bind.oid, data + processed + 2, pkt->var_bind.oid_length);
    processed += 2 + pkt->var_bind.oid_length;

    if (*((char *)data + processed) == 5 && *((char *)data + processed + 1) == 0)
    {
        return 1;
    }

    pkt->var_bind.oid_data_type = *((char *)data + processed);
    pkt->var_bind.oid_data_length = *((char *)data + processed + 1);
    memcpy(pkt->var_bind.oid_data, data + processed + 2, pkt->var_bind.oid_data_length);
    processed += 2 + pkt->var_bind.oid_data_length;

    return 1;
}

int snmp_header_pack(struct snmp_header *header, void *packed)
{
    int header_size = 0;

    //ПОле Version
    *((char *)packed) = 2;
    *((char *)packed + 1) = 1;
    *((char *)packed + 2) = header->version;
    header_size += 3;

    //Поле Community
    *((char *)packed + 3) = 4;
    *((char *)packed + 4) = header->community_len;
    memcpy((char *)packed + 5, header->community, header->community_len);
    header_size += 2 + header->community_len;

    //Поле PDU-type
    switch (header->pdu_type)
    {
    case 0:
        *((char *)packed + header_size) = 160;
        break;
    case 1:
        *((char *)packed + header_size) = 161;
        break;
    case 2:
        *((char *)packed + header_size) = 162;
        break;
    case 3:
        *((char *)packed + header_size) = 163;
        break;
    default:
        break;
    }
    header_size += 2;
    return header_size;
}

int snmp_pdu_pack(struct snmp_pdu *pdu, void *packed)
{
    int pdu_size = 0;

    //Поле id
    *((char *)packed) = 2;
    *((char *)packed + 1) = 4;
    pdu->id = htons(pdu->id);
    memcpy((char *)packed + 2, pdu, 4);
    pdu_size += 6;

    //Поле статус ошибки
    *((char *)packed + pdu_size) = 2;
    *((char *)packed + pdu_size + 1) = 1;
    *((char *)packed + pdu_size + 2) = pdu->error_status;
    pdu_size += 3;

    //Поле индекс ошибки
    *((char *)packed + pdu_size) = 2;
    *((char *)packed + pdu_size + 1) = 1;
    *((char *)packed + pdu_size + 2) = pdu->error_index;
    pdu_size += 3;

    return pdu_size;
}

int snmp_packet_pack(struct snmp_packet *pkt, void *packed)
{
    int packed_len = 0;
    *((char *)packed) = 48;
    *((char *)packed + 1) = 1;

    //Pack header
    int header_size = snmp_header_pack(&pkt->header, packed + 3);

    //Pack pdu
    int pdu_size = snmp_pdu_pack(&pkt->pdu, packed + 3 + header_size);

    //Pack OID
    *((char *)packed + header_size + pdu_size + 3) = 48;
    *((char *)packed + header_size + pdu_size + 4) = pkt->var_bind.oid_length + pkt->var_bind.oid_data_length + 4;
    packed_len += 4 + header_size + pdu_size;

    *((char *)packed + packed_len) = 6;
    *((char *)packed + packed_len + 1) = pkt->var_bind.oid_length;
    memcpy(packed + packed_len + 2, pkt->var_bind.oid, pkt->var_bind.oid_length);
    packed_len += 2 + pkt->var_bind.oid_length;
    if (pkt->var_bind.oid_data_length == 0)
    {
        *((char *)packed + packed_len) = 5;
        *((char *)packed + packed_len + 1) = 0;
        packed_len += 2;
    }
    else
    {
        *((char *)packed + packed_len) = pkt->var_bind.oid_data_type;
        *((char *)packed + packed_len + 1) = pkt->var_bind.oid_data_length;
        memcpy(packed + packed_len + 2, pkt->var_bind.oid_data, pkt->var_bind.oid_data_length);
        packed_len += 2 + pkt->var_bind.oid_data_length;
    }

    //Fill lengths
    *((char *)packed + 2) = packed_len - 2;
    *((char *)packed + header_size + 2) = packed_len - header_size - 2;

    return packed_len;
}