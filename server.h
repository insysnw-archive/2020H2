#ifndef __SERVER_H__
#define __SERVER_H__

#define PACKET_SIZE 252

struct dns_config
{
	char *host;
	int port;
};

struct dns_server
{
	struct dns_config config;
	int listenfd;
};

extern struct dns_server dns_server;

void dns_init (void);
void dns_start (void);
void dns_loop (void);


#endif
