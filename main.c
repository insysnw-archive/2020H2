#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <assert.h>
#include <syslog.h>

#include "server.h"

struct dns_server dns_server;

int main(int argc, char **argv)
{
	int o;

	setbuf(stdout, NULL);
	setbuf(stderr, NULL);

	dns_init();

	while ((o = getopt(argc, argv, "h:p:")) != -1)
		switch (o)
		{
		case 'h':
			dns_server.config.host = optarg;
			break;
		case 'p':
			assert(dns_server.config.port = atoi(optarg));
			break;
		default:
			abort();
		}

	dns_start();
	dns_loop();

	return 0;
}
