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

#include "protocol.h"

#define DEF_PORT "8888" // Port we're listening on
#define ADMIN_LOGIN "admin"
#define ADMIN_PASSWORD "admin"

int sockets[256];

struct user
{
    char *name;
    char *dir;
    int isAdmin;
};

struct user *user_array[256];

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

void *clientHandler(void *args)
{
    int sock = *(int *)args;
    struct user usr;

    for (;;)
    {
        struct protocol_packet *pkt = calloc(1, sizeof(struct protocol_packet));
        char buf[256];
        int res = recv(sock, buf, 256, 0);

        printf("received packet! %d\n", res);

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

            for (int i = 0; i < 256; i++)
            {
                if (sockets[i] == sock)
                {
                    sockets[i] = -1;
                    return;
                    break;
                }
            }
        }

        packet_parse(pkt, buf, res);

        packet_print(pkt);
        if (pkt->request.command != COMMAND_LOGIN)
        {
            int f = 0;
            for (int i = 0; i < 256; i++)
            {
                if (user_array[i] != -1)
                {
                    if (strcmp(user_array[i]->name, usr.name) == 0)
                    {
                        f = 1;
                    }
                }
            }
            if (f == 0)
            {
                struct protocol_packet *new_pkt = calloc(1, sizeof(struct protocol_packet));
                new_pkt->response.error_code = 2;
                new_pkt->response.arg_size = 0;
                new_pkt->qr = 1;
                int s = response_pack(new_pkt, buf);

                res = send(sock, buf, s, 0);
                return;
            }
        }

        int len;
        char arg[256];
        bzero(arg, 256);
        FILE *fp;
        struct protocol_packet *back_pkt = calloc(1, sizeof(struct protocol_packet));

        back_pkt->qr = CODE_RESPONSE;
        back_pkt->response.error_code = 0;

        switch (pkt->request.command)
        {
        case COMMAND_LS:;
            char ch[] = "cd ";
            strcat(ch, usr.dir);
            strcat(ch, " && ls");
            printf("Command: %s", ch);
            fp = popen(ch, "r");
            int k = 0;
            char *ch2;

            while ((ch2 = fgetc(fp)) != EOF)
            {
                *(arg + k) = ch2;
                k++;
            }

            back_pkt->response.arg_size = k + 1;
            back_pkt->response.args = calloc(back_pkt->response.arg_size, sizeof(char));
            pack_arg(back_pkt->response.args, arg, RESPONSE_STRING);
            break;
        case COMMAND_CD:
            len = parse_arg(arg, pkt->request.args);

            char c[] = "cd ";
            strcat(c, usr.dir);
            strcat(c, " && cd ");
            strcat(c, arg);
            printf("Command: %s", c);
            char *ch3;
            int j = 0;
            fp = popen(c, "r");
            while ((ch3 = fgetc(fp)) != EOF)
            {
                *(arg + j) = ch3;
                j++;
            }

            strcat(c, " && pwd");
            fp = popen(c, "r");
            j = 0;
            while ((ch3 = fgetc(fp)) != EOF)
            {
                *(arg + j) = ch3;
                j++;
            }

            usr.dir = malloc(j - 1);

            memcpy(usr.dir, arg, j - 1);

            back_pkt->response.arg_size = 0;
            bzero(arg, 256);
            break;
        case COMMAND_WHO:;
            char result[] = "";
            for (int i = 0; i < 256; i++)
            {
                if (user_array[i] != -1)
                {
                    strcat(result, "user    ");
                    strcat(result, user_array[i]->name);
                    strcat(result, "    ");
                    strcat(result, user_array[i]->dir);
                    strcat(result, "\n");
                }
            }
            printf("%s\n", result);
            back_pkt->response.arg_size = strlen(result) + 2;
            back_pkt->response.args = calloc(back_pkt->response.arg_size, sizeof(char));

            pack_arg(back_pkt->response.args, result, RESPONSE_STRING);
            printf("%d\n", back_pkt->response.arg_size);
            break;
        case COMMAND_LOGOUT:
            for (int i = 0; i < 256; i++)
            {
                if (user_array[i] != -1)
                {
                    if (strcmp(user_array[i]->name, usr.name) == 0)
                    {
                        user_array[i] = -1;
                    }
                }
            }
            back_pkt->response.arg_size = 0;
            break;
        case COMMAND_LOGIN:;
            char login[256], password[256];
            int login_size = parse_arg(login, pkt->request.args);

            int isAdmin = 0;

            if (login_size < pkt->request.arg_size)
            {
                if (strcmp(login, ADMIN_LOGIN) == 0)
                {
                    int pass_size = parse_arg(password, pkt->request.args + login_size);
                    printf("log: %s\npass: %s\n", login, password);
                    if (strcmp(password, ADMIN_PASSWORD) == 0)
                    {
                        isAdmin = 1;
                    }
                }
            }

            FILE *fp;
            usr.isAdmin = isAdmin;

            usr.name = calloc(sizeof(login), sizeof(char));
            memcpy(usr.name, login, sizeof(login));

            char m[] = "pwd";
            fp = popen(m, "r");

            fscanf(fp, "%s", buf);

            usr.dir = malloc(sizeof(buf));

            memcpy(usr.dir, buf, sizeof(buf));

            for (int i = 0; i < 256; i++)
            {
                if (user_array[i] != -1)
                {
                    struct user *u = user_array[i];
                    if (strcmp(u->name, usr.name) == 0)
                    {
                        close(sock);
                        return;
                    }
                }
            }
            printf("after\n");

            for (int i = 0; i < 256; i++)
            {
                if (user_array[i] == -1)
                {
                    user_array[i] = &usr;
                    break;
                }
            }
            back_pkt->response.arg_size = 0;
            break;
        case COMMAND_KILL:
            if (usr.isAdmin == 0)
            {
                back_pkt->response.error_code = 10;
            }
            else
            {
                char username[256];
                bzero(username, 256);
                int username_len = parse_arg(username, pkt->request.args);
                printf("pass\n");
                for (int i = 0; i < 256; i++)
                {
                    if (user_array[i] != -1)
                    {
                        if (strcmp(user_array[i]->name, username) == 0)
                        {
                            user_array[i] = -1;
                        }
                    }
                }
            }
            back_pkt->response.arg_size = 0;
            break;
        default:
            break;
        }
        packet_print(back_pkt);
        int req_size = response_pack(back_pkt, buf);

        res = send(sock, buf, req_size, 0);
        free(back_pkt);
    }
}

// Main
int main(int argc, char **argv)
{
    int listener; // Listening socket descriptor

    for (int i = 0; i < 256; i++)
    {
        user_array[i] = -1;
    }

    int newfd;
    struct sockaddr_storage remoteaddr;
    socklen_t addrlen;

    char remoteIP[INET6_ADDRSTRLEN];

    int o;
    char *port;

    for (int i = 0; i < 256; i++)
    {
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
            for (int i = 0; i < 256; i++)
            {
                if (sockets[i] == -1)
                {
                    sockets[i] = newfd;
                    pthread_t clientH;
                    int res = pthread_create(&clientH, NULL, clientHandler, (void *)(&newfd));
                    if (res)
                    {
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
