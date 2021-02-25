#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <poll.h>
#include <fcntl.h>
#include <time.h>
#include <string.h>

#include "chat_protocol.h"

#define DEF_PORT "8888"
#define DEF_IP "127.0.0.1"
#define DEF_NICK "Anonymous"
#define MAXDATASIZE 100

// get sockaddr, IPv4 or IPv6:
void *get_in_addr(struct sockaddr *sa)
{
	if (sa->sa_family == AF_INET)
	{
		return &(((struct sockaddr_in *)sa)->sin_addr);
	}

	return &(((struct sockaddr_in6 *)sa)->sin6_addr);
}

int main(int argc, char **argv)
{
	char *addr, *port, *nick;
	int numbytes;
	int id;
	int o;

	port = DEF_PORT;
	addr = DEF_IP;
	nick = DEF_NICK;

	while ((o = getopt(argc, argv, "p:a:n:")) != -1)
	{
		switch (o)
		{
		case 'p':
			port = optarg;
			break;
		case 'a':
			addr = optarg;
			break;
		case 'n':
			nick = optarg;
			break;
		default:
			abort();
		}
	}

	// создаем сокет
	struct addrinfo hints, *servinfo, *p;
	int rv;
	char s[INET6_ADDRSTRLEN];
	int sockfd;

	memset(&hints, 0, sizeof hints);
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;

	if ((rv = getaddrinfo(addr, port, &hints, &servinfo)) != 0)
	{
		fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
		return 1;
	}

	// loop through all the results and connect to the first we can
	for (p = servinfo; p != NULL; p = p->ai_next)
	{
		if ((sockfd = socket(p->ai_family, p->ai_socktype,
							 p->ai_protocol)) == -1)
		{
			perror("client: socket");
			continue;
		}

		if (connect(sockfd, p->ai_addr, p->ai_addrlen) == -1)
		{
			close(sockfd);
			perror("client: connect");
			continue;
		}

		break;
	}

	if (p == NULL)
	{
		fprintf(stderr, "client: failed to connect\n");
		return 2;
	}

	inet_ntop(p->ai_family, get_in_addr((struct sockaddr *)p->ai_addr),
			  s, sizeof s);
	printf("client: connecting to %s\n", s);

	freeaddrinfo(servinfo); // all done with this structure

	char buf[MAXDATASIZE];

	struct pollfd *pfds = malloc(sizeof *pfds * 2);
	pfds[0].fd = 0; //Ввод пользователя
	pfds[0].events = POLLIN;

	pfds[1].fd = sockfd; //Сервер
	pfds[1].events = POLLIN;

	printf("Connected to server!\nTo send message simply press enter!\nTO finish session enter an empty string\n");

	//Основной цикл
	while (1)
	{
		bzero(buf, MAXDATASIZE);
		int poll_count = poll(pfds, 2, -1);

		if (poll_count == -1)
		{
			perror("poll");
			exit(1);
		}

		if (pfds[0].revents & POLLIN)
		{
			pfds[0].revents = 0;
			//Пользователь ввел сообщение, пытаемся послать
			//Для начала пытаемся его прочитать
			numbytes = read(pfds[0].fd, buf, sizeof buf);

			if (numbytes < 0)
			{
				perror("read_user_input");
				printf("Error occured while trying to read user input\n");
				continue;
			}

			if (numbytes - 1 == 0)
			{
				close(sockfd);
				return 0;
			}

			buf[numbytes - 1] = 0;

			//Прочитать удалось, отсылаем
			struct chat_packet *pkt;
			pkt = calloc(1, sizeof(struct chat_packet));
			pkt->time = 0;
			pkt->nick_size = strlen(nick);
			pkt->nick = calloc(strlen(nick), sizeof(char));
			memcpy(pkt->nick, nick, strlen(nick));
			pkt->data_size = strlen(buf);
			pkt->data = malloc(sizeof(buf));
			memcpy(pkt->data, buf, strlen(buf));
			chat_packet_send(pkt, pfds[1].fd);
			free(pkt->nick);
			free(pkt->data);
			free(pkt);
		}
		else
		{
			pfds[1].revents = 0;
			struct chat_packet *pkt;

			//Пришло сообщение от сервера, пытаемся прочитать
			pkt = calloc(1, sizeof(struct chat_packet));
			int res = chat_packet_receive(pkt, pfds[1].fd);
			if (res == 0)
			{
				printf("server is offline\n");
				return 0;
			}
			//Прочитать удалось, отображаем
			int i = 0;

			time_t rawtime = (time_t)pkt->time;

			struct tm *ptm = localtime(&rawtime);

			printf("<%02d:%02d> [%s] %s\n", ptm->tm_hour, ptm->tm_min, pkt->nick, pkt->data);

			free(pkt->data);
			free(pkt->nick);
			free(pkt);
		}
	}
	return 0;
}