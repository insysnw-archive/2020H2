#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <poll.h>
#include <time.h>
#include <pthread.h>


#include "chat_protocol.h"

#define DEF_PORT "8888" // Port we're listening on

int sockets[256];

// Get sockaddr, IPv4 or IPv6:
void *get_in_addr(struct sockaddr *sa)
{
    if (sa->sa_family == AF_INET)
    {
        return &(((struct sockaddr_in *)sa)->sin_addr);
    }

    return &(((struct sockaddr_in6 *)sa)->sin6_addr);
}

// Return a listening socket
int get_listener_socket(char *port)
{
    int listener; // Listening socket descriptor
    int yes = 1;  // For setsockopt() SO_REUSEADDR, below
    int rv;

    struct addrinfo hints, *ai, *p;

    // Get us a socket and bind it
    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = AI_PASSIVE;
    if ((rv = getaddrinfo(NULL, port, &hints, &ai)) != 0)
    {
        fprintf(stderr, "selectserver: %s\n", gai_strerror(rv));
        exit(1);
    }

    for (p = ai; p != NULL; p = p->ai_next)
    {
        listener = socket(p->ai_family, p->ai_socktype, p->ai_protocol);
        if (listener < 0)
        {
            continue;
        }

        // Lose the pesky "address already in use" error message
        setsockopt(listener, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int));

        if (bind(listener, p->ai_addr, p->ai_addrlen) < 0)
        {
            close(listener);
            continue;
        }

        break;
    }

    freeaddrinfo(ai); // All done with this

    // If we got here, it means we didn't get bound
    if (p == NULL)
    {
        return -1;
    }

    // Listen
    if (listen(listener, 10) == -1)
    {
        return -1;
    }

    return listener;
}

void* clientHandler(void* args){
	int sock = *(int*) args;
	
	for(;;) {
		struct chat_packet *pkt = calloc(1, sizeof(struct chat_packet));
        int res = chat_packet_receive(pkt, sock);
		printf("received packet!\n");

		if (res <= 0)
                    {
                        if (res == 0)
                        {
                            printf("pollserver: socket %d hung up\n", sock);
                        }
                        else
                        {
                            perror("recv");
                        }

                        close(sock);

                        for (int i = 0; i < 256; i++) {
							if (sockets[i] == sock) {
								sockets[i] = -1;
								return;
								break;
							}
						}
                    }

                            time_t rawtime;
                            time(&rawtime);
                            pkt->time = rawtime;

                            chat_print_packet(pkt);
		for (int i = 0; i < 256; i++) {
			if (sockets[i] != -1 && sockets[i] != sock){
				chat_packet_send(pkt, sockets[i]);
			}
		}
	}
}

// Main
int main(int argc, char **argv)
{
    int listener; // Listening socket descriptor

    int newfd;
    struct sockaddr_storage remoteaddr;
    socklen_t addrlen;

    char remoteIP[INET6_ADDRSTRLEN];

    int o;
    char *port;
	
	for (int i =0; i < 256; i++){
		sockets[i] = -1;
	}

    while ((o = getopt(argc, argv, "p:")) != -1)
    {
        switch (o)
        {
        case 'p':
            port = optarg;
            break;
        default:
            abort();
        }
    }

    listener = get_listener_socket(port);
	

    if (listener == -1)
    {
        fprintf(stderr, "error getting listening socket\n");
        exit(1);
    }
	
    for (;;)
    {
                    addrlen = sizeof remoteaddr;
                    newfd = accept(listener,
                                   (struct sockaddr *)&remoteaddr,
                                   &addrlen);

                    if (newfd == -1)
                    {
                        perror("accept");
                    }
                    else
                    {
                        for (int i =0; i  < 256; i++){
							if (sockets[i] == -1){
								sockets[i] = newfd;
								pthread_t clientH;
    int res = pthread_create(&clientH, NULL, clientHandler, (void*)(&newfd));
    if (res){
        printf("Error while creating new thread\n");
    }
	break;
							}
						}

                        printf("pollserver: new connection from %s on "
                               "socket %d\n",
                               inet_ntop(remoteaddr.ss_family,
                                         get_in_addr((struct sockaddr *)&remoteaddr),
                                         remoteIP, INET6_ADDRSTRLEN),
                               newfd);
                    }

    }

    return 0;
}
