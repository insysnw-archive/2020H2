#include <arpa/inet.h>

#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <errno.h>
#include <string.h>

#include "dns_protocol.h"

void dns_print_rr(struct dns_resource_record *rr)
{
	printf("%s		%d		%d		%d		%s", rr->name, rr->ttl, rr->class, rr->type, rr->rdata);
}

void dns_print_header(struct dns_header *header)
{
	/*printf("ID: %d\n", header->id);
	printf("qr: %d\n", header->qr);
	printf("opcode: %d\n", header->opcode);
	printf("aa: %d\n", header->aa);
	printf("tc: %d\n", header->tc);
	printf("rd: %d\n", header->rd);
	printf("ra: %d\n", header->ra);
	printf("z: %d\n", header->z);
	printf("rcode: %d\n", header->rcode);*/

	printf("qdcount: %d\n", header->qdcount);
	printf("ancount: %d\n", header->ancount);
	printf("nscount: %d\n", header->nscount);
	printf("arcount: %d\n", header->arcount);
}

void dns_print_question(struct dns_question *q)
{
	printf("qname: %s\n", q->qname);
	printf("qtype: %d\n", q->qtype);
	printf("qclass: %d\n", q->qclass);
}

void dns_print_packet(struct dns_packet *packet)
{
	dns_print_header(&packet->header);
	dns_print_question(&packet->question);
	printf("data_size: %d\n", packet->data_size);
	printf("data: %s\n", packet->data);
}

int dns_request_parse(struct dns_packet *pkt, void *data, u_int16_t size)
{
	dns_header_parse(&pkt->header, data);
	int question_len = 0;
	if (pkt->header.qdcount == 1)
	{
		question_len = question_len + dns_question_parse(&pkt->question, data + 12, size - 12);
	}

	if (12 + question_len < size)
	{
		pkt->data = calloc(size - 12 - question_len, sizeof(char));
		memcpy(pkt->data, data + 12 + question_len, size - 12 - question_len);
		pkt->data_size = size - 12 - question_len;
	}
	else
	{
		pkt->data_size = 0;
	}

	return 1;
}

int dns_header_parse(struct dns_header *header, void *data)
{
	memcpy(header, data, 12);

	header->id = ntohs(header->id);
	header->qdcount = ntohs(header->qdcount);
	header->ancount = ntohs(header->ancount);
	header->nscount = ntohs(header->nscount);
	header->arcount = ntohs(header->arcount);
	header->flags = ntohs(header->flags);

	return 1;
}

int dns_question_parse(struct dns_question *q, void *data, u_int16_t size)
{
	char *temp = malloc(size);
	memcpy(temp, data, size);

	int k = 1;
	char *name_temp;
	name_temp = calloc(temp[0], sizeof(char));
	memcpy(name_temp, data + k, temp[0]);
	int i = temp[0];

	while (temp[i + k] != 0)
	{
		//strcat(name_temp, ".");
		char *t = calloc(strlen(name_temp) + 1, sizeof(char));
		memcpy(t, name_temp, strlen(name_temp));
		t[strlen(name_temp)] = '.';
		free(name_temp);
		name_temp = calloc(strlen(t), sizeof(char));
		memcpy(name_temp, t, strlen(t));
		free(t);

		k++;

		t = calloc(temp[i + k - 1], sizeof(char));

		memcpy(t, temp + i + k, temp[i + k - 1]);
		//strcat(name_temp, t);
		char *t2 = calloc(strlen(t) + strlen(name_temp), sizeof(char));
		memcpy(t2, name_temp, strlen(name_temp));
		memcpy(t2 + strlen(name_temp), t, strlen(t));
		free(name_temp);
		name_temp = calloc(strlen(t2), sizeof(char));
		memcpy(name_temp, t2, strlen(t2));

		free(t2);
		free(t);
		i += temp[i + k - 1];
	}
	i++;

	q->qname = calloc(strlen(name_temp), sizeof(char));
	q->qname_len = strlen(name_temp);
	memcpy(q->qname, name_temp, strlen(name_temp));
	memcpy(q, data + i + k, 4);
	q->qtype = ntohs(q->qtype);
	q->qclass = ntohs(q->qclass);

	free(temp);

	return i + k + 4;
}

int dns_packet_pack(struct dns_packet *pkt, void *packed)
{
	//Pack header
	pkt->header.id = htons(pkt->header.id);
	pkt->header.qdcount = htons(pkt->header.qdcount);
	pkt->header.ancount = htons(pkt->header.ancount);
	pkt->header.nscount = htons(pkt->header.nscount);
	pkt->header.arcount = htons(pkt->header.arcount);

	pkt->header.flags = htons(pkt->header.flags);

	memcpy(packed, &pkt->header, 12);
	int buf_len = 12;

	//Pack question
	u_int8_t count = 0;

	int i = 0;

	while (i < pkt->question.qname_len)
	{
		if (*(pkt->question.qname + i) == '.')
		{
			memcpy(packed + buf_len, &count, 1);
			buf_len++;
			memcpy(packed + buf_len, pkt->question.qname + i - count, count);
			buf_len += count;
			count = 0;
		}
		else
		{
			count++;
		}
		i++;
	}

	memcpy(packed + buf_len, &count, 1);
	buf_len++;
	memcpy(packed + buf_len, pkt->question.qname + i - count, count);
	buf_len += count;
	count = 0;
	*((char *)packed + buf_len) = 0;
	buf_len += 1;

	pkt->question.qclass = htons(pkt->question.qclass);
	pkt->question.qtype = htons(pkt->question.qtype);
	memcpy(packed + buf_len, &pkt->question, 4);
	buf_len += 4;

	i = 0;
	//Pack answer

	//Pack pointer to name
	char temp[2];
	temp[0] = 192;
	temp[1] = 12;
	memcpy(packed + buf_len, &temp, 2);
	buf_len += 2;
	/*memcpy(packed + buf_len, pkt->rr.name+i - count, count);
	buf_len += count;
	count = 0;
	*((char*)packed+buf_len) = 0;
	buf_len+=1;*/

	int type = pkt->rr.type;
	pkt->rr.type = htons(pkt->rr.type);
	pkt->rr.class = htons(pkt->rr.class);
	pkt->rr.ttl = htons(pkt->rr.ttl);
	if (type == 16)
	{
		pkt->rr.rdlength = pkt->rr.rdlength + 1;
	}
	u_int16_t rr_len = pkt->rr.rdlength;
	if (type == 16)
	{
		rr_len--;
	}
	pkt->rr.rdlength = htons(pkt->rr.rdlength);

	memcpy(packed + buf_len, &pkt->rr, 10);
	buf_len += 10;

	if (type == 16)
	{
		u_int8_t txt_size = rr_len;
		memcpy(packed + buf_len, &txt_size, 1);
		buf_len += 1;
	}

	memcpy(packed + buf_len, pkt->rr.rdata, rr_len);
	buf_len += rr_len;

	return buf_len;
}