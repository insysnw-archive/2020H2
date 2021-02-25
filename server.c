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
#include <fcntl.h>

#include "chat_protocol.h"

#define DEF_PORT "8888" // Port we're listening on

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

// Add a new file descriptor to the set
void add_to_pfds(struct pollfd *pfds[], int newfd, int *fd_count, int *fd_size)
{
    // If we don't have room, add more space in the pfds array
    if (*fd_count == *fd_size)
    {
        *fd_size *= 2; // Double it

        *pfds = realloc(*pfds, sizeof(**pfds) * (*fd_size));
    }

    (*pfds)[*fd_count].fd = newfd;
    (*pfds)[*fd_count].events = POLLIN; // Check ready-to-read

    (*fd_count)++;
}

// Remove an index from the set
void del_from_pfds(struct pollfd pfds[], int i, int *fd_count)
{
    // Copy the one from the end over this one
    pfds[i] = pfds[*fd_count - 1];

    (*fd_count)--;
}

// Main
int main(int argc, char **argv)
{
    int listener; // Listening socket descriptor

    int newfd;
    struct sockaddr_storage remoteaddr;
    socklen_t addrlen;

    char buf[256]; // Buffer for client data

    char remoteIP[INET6_ADDRSTRLEN];

    struct chat_packet *pkt;

    int fd_count = 0;
    int fd_size = 5;
    struct pollfd *pfds = malloc(sizeof *pfds * fd_size);

    int o;
    char *port;

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

    fcntl(listener, F_SETFL, O_NONBLOCK);

    pfds[0].fd = listener;
    pfds[0].events = POLLIN;

    fd_count = 2;

    for (;;)
    {
        int poll_count = poll(pfds, fd_count, -1);

        if (poll_count == -1)
        {
            perror("poll");
            exit(1);
        }

        for (int i = 0; i < fd_count; i++)
        {

            if (pfds[i].revents & POLLIN)
            {

                if (pfds[i].fd == listener)
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
                        fcntl(newfd, F_SETFL, O_NONBLOCK);
                        add_to_pfds(&pfds, newfd, &fd_count, &fd_size);

                        printf("pollserver: new connection from %s on "
                               "socket %d\n",
                               inet_ntop(remoteaddr.ss_family,
                                         get_in_addr((struct sockaddr *)&remoteaddr),
                                         remoteIP, INET6_ADDRSTRLEN),
                               newfd);
                    }
                }
                else
                {
                    pkt = calloc(1, sizeof(struct chat_packet));
                    int res = chat_packet_receive(pkt, pfds[i].fd);

                    int sender_fd = pfds[i].fd;

                    if (res <= 0)
                    {
                        if (res == 0)
                        {
                            printf("pollserver: socket %d hung up\n", sender_fd);
                        }
                        else
                        {
                            perror("recv");
                        }

                        close(pfds[i].fd);

                        del_from_pfds(pfds, i, &fd_count);
                    }
                    else
                    {
                        // Got message from client
                        int i = 0;

                        time_t rawtime;
                        time(&rawtime);
                        pkt->time = rawtime;

                        chat_print_packet(pkt);
                        for (int j = 0; j < fd_count; j++)
                        {

                            int dest_fd = pfds[j].fd;

                            // Except the listener and ourselves
                            if (dest_fd != listener && dest_fd != sender_fd)
                            {
                                chat_packet_send(pkt, dest_fd);
                            }
                        }
                        free(pkt->data);
                        free(pkt);
                    }
                }
            }
        }
    }

    return 0;
}