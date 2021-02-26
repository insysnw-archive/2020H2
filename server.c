#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <syslog.h>
#include <errno.h>
#include <fcntl.h>

#include "dns_protocol.h"
#include "server.h"

struct dns_server dns_server;

void dns_init(void)
{
	printf("Initializing DNS server.\n");

	dns_server.config.port = 53;
	dns_server.config.host = "127.0.0.1";

	dns_server.listenfd = 0;
}

//Запускаем сервер
void dns_start(void)
{
	printf("Starting DNS server.\n");

	struct sockaddr_in sin;
	int sd;

	printf("Opening sockets.\n");

	assert((sd = socket(AF_INET, SOCK_DGRAM, 0)) > 0);

	memset((char *)&sin, 0, sizeof(sin));
	sin.sin_family = AF_INET;
	sin.sin_port = htons(dns_server.config.port);
	sin.sin_addr.s_addr = inet_addr(dns_server.config.host);
	assert(bind(sd, (struct sockaddr *)&sin, sizeof(sin)) == 0);

	dns_server.listenfd = sd;
}

//Обрабатываем запросы в цикле
void dns_loop(void)
{
	int req_size;
	char buf[PACKET_SIZE + 4];
	socklen_t from_len;
	struct sockaddr_in from;
	struct dns_packet *pkt;

	from_len = sizeof(from);

	printf("Accepting connections...\n");
	for (;;)
	{
		req_size = recvfrom(dns_server.listenfd, buf, PACKET_SIZE + 4, 0, (struct sockaddr *)&from, &from_len);
		printf("client: %s %d\n", strerror(errno), req_size);

		pkt = calloc(1, sizeof(struct dns_packet));
		dns_request_parse(pkt, buf, req_size);

		//dns_print_packet(pkt);

		if (pkt->question.qtype == 1)
		{
			pkt->rr.name = pkt->question.qname;
			pkt->rr.type = 1;
			pkt->rr.class = 1;
			pkt->rr.ttl = 540;
			pkt->rr.rdlength = 4;
			pkt->rr.rdata = calloc(4, sizeof(char));
			memcpy(pkt->rr.rdata, "0000", 4);
			pkt->header.flags = 0b1000000000000000;
			pkt->header.ancount = 1;
		}

		if (pkt->question.qtype == 15)
		{
			pkt->rr.name = pkt->question.qname;
			pkt->rr.type = 15;
			pkt->rr.class = 1;
			pkt->rr.ttl = 540;
			char mail_buf[10];
			mail_buf[0] = 2;
			mail_buf[1] = 'm';
			mail_buf[2] = 'x';
			mail_buf[3] = 2;
			mail_buf[4] = 'y';
			mail_buf[5] = 'a';
			mail_buf[6] = 2;
			mail_buf[7] = 'r';
			mail_buf[8] = 'u';
			mail_buf[9] = 0;
			pkt->rr.rdlength = 12;
			pkt->rr.rdata = calloc(pkt->rr.rdlength, sizeof(char));
			char pref[2];
			pref[0] = 0;
			pref[1] = 10;
			memcpy(pkt->rr.rdata, pref, 2);
			memcpy(pkt->rr.rdata + 2, mail_buf, 10);
			pkt->header.flags = 0b1000000000000000;
			pkt->header.ancount = 1;
		}

		if (pkt->question.qtype == 16)
		{
			pkt->rr.name = pkt->question.qname;
			pkt->rr.type = 16;
			pkt->rr.class = 1;
			pkt->rr.ttl = 540;
			char *buf = "Hello, this is example txt on server";
			pkt->rr.rdlength = strlen(buf);
			pkt->rr.rdata = calloc(strlen(buf), sizeof(char));
			memcpy(pkt->rr.rdata, buf, strlen(buf));
			pkt->header.flags = 0b1000000000000000;
			pkt->header.ancount = 1;
		}

		if (pkt->question.qtype == 28)
		{
			pkt->rr.name = pkt->question.qname;
			pkt->rr.type = 28;
			pkt->rr.class = 1;
			pkt->rr.ttl = 540;
			pkt->rr.rdlength = 16;
			pkt->rr.rdata = calloc(4, sizeof(char));
			memcpy(pkt->rr.rdata, "0000000000000000", 16);
			pkt->header.flags = 0b1000000000000000;
			pkt->header.ancount = 1;
		}

		req_size = dns_packet_pack(pkt, buf);
		printf("resulting packet size:%d\n", req_size);

		//dns_request_parse(pkt, buf, req_size);
		//dns_print_packet(pkt);

		sendto(dns_server.listenfd, buf, req_size, 0, (struct sockaddr *)&from, from_len);

		free(pkt->data);
		free(pkt);
	}
}
