/*
	Simple udp client
*/
#include <arpa/inet.h>
#include <netinet/in.h>
#include <netdb.h>
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
#include <pthread.h>

#include "snmp_protocol.h"

#define PORT "9999"

struct handler_agrs
{
    int sock;
    struct addrinfo *p2;
};

void *response_handler(void *args)
{
    struct handler_agrs *h_a = (struct handler_agrs *)args;
    char buf[256];
    int req_size = recvfrom(h_a->sock, buf, 256, 0, h_a->p2->ai_addr, &h_a->p2->ai_addrlen);
    struct snmp_packet *pkt = calloc(1, sizeof(struct snmp_packet));
    snmp_packet_parse(pkt, buf, req_size);
    snmp_print_packet(pkt);
}

int main(int argc, char *argv[])
{

    char *port = PORT;

    int o;

    while ((o = getopt(argc, argv, "h:p:")) != -1)
        switch (o)
        {
        case 'p':
            port = optarg;
            break;
        default:
            abort();
        }

    printf("Snmp client started\nTo exit type quit\nType \"get [name]\" to send get request or \"set [name] [int or str] [value]\" to send set request\nTo change agent address type \"agent [IPv4]\"\n");

    char *agent = "127.0.0.1";
    int flag = -1;
    struct snmp_var_bind *var_bind;
    struct snmp_packet *pkt;
    struct snmp_header *header;
    struct snmp_pdu *pdu;
    u_int32_t id;

    for (;;)
    {
        var_bind = malloc(sizeof(struct snmp_var_bind));
        header = malloc(sizeof(struct snmp_header));
        header->version = 0;
        header->community_len = 6;
        header->community = calloc(header->community_len, sizeof(char));
        memcpy(header->community, "public", header->community_len);

        char *p = malloc(256);
        int i = 0;
        flag = 0;
        //printf("before input\n");

        //Read user input
        while ((*p = getchar()))
        {
            if (*p == ' ' || *p == '\n')
            {
                if (flag == 6 || flag == 7)
                {
                    flag = -1;
                    break;
                }
                char *t = calloc(i, sizeof(char));
                memcpy(t, p - i, i);
                if (flag == 0)
                {
                    if (strcmp(t, "get") == 0)
                    {
                        flag = 1;
                        header->pdu_type = 0;
                    }
                    else
                    {
                        if (strcmp(t, "set") == 0)
                        {
                            flag = 2;
                            header->pdu_type = 3;
                        }
                        else
                        {
                            if (strcmp(t, "agent") == 0)
                            {
                                flag = 5;
                            }
                            else
                            {
                                if (strcmp(t, "quit") == 0)
                                {
                                    return 1;
                                }
                                else
                                {
                                    flag = -1;
                                    break;
                                }
                            }
                        }
                    }
                }
                else
                {
                    switch (flag)
                    {
                    case 1:
                        var_bind->oid_length = i;
                        var_bind->oid = calloc(i, sizeof(char));
                        memcpy(var_bind->oid, t, i);
                        flag = 6;
                        break;
                    case 2:
                        var_bind->oid_length = i;
                        var_bind->oid = calloc(i, sizeof(char));
                        memcpy(var_bind->oid, t, i);
                        flag = 3;
                        break;
                    case 3:
                        if (strcmp(t, "int") == 0)
                        {
                            var_bind->oid_data_type = DATA_TYPE_INT;
                        }
                        else
                        {
                            if (strcmp(t, "str") == 0)
                            {
                                var_bind->oid_data_type = DATA_TYPE_OCTET_STRING;
                            }
                            else
                            {
                                flag = 6;
                                break;
                            }
                        }
                        flag = 4;
                        break;
                    case 4:
                        var_bind->oid_data_length = i;
                        var_bind->oid_data = calloc(i, sizeof(char));
                        memcpy(var_bind->oid_data, t, i);
                        flag = 6;
                        break;
                    case 5:
                        agent = malloc(sizeof(t));
                        memcpy(agent, t, sizeof(t));
                        flag = 7;
                        break;
                    default:
                        break;
                    }
                    if (flag == 6 || flag == 7)
                    {
                        break;
                    }
                }
                free(t);
                i = 0;
            }
            else
            {
                i++;
            }
            p++;
        }
        if (flag < 6)
        {
            printf("Can't understand your input, try again\n");
            continue;
        }

        if (flag == 7)
        {
            printf("Server changed\n");
            continue;
        }

        //Send packet
        pkt = calloc(1, sizeof(struct snmp_packet));
        pdu = malloc(sizeof(struct snmp_pdu));
        pdu->id = id;
        pdu->error_status = 0;
        pdu->error_index = 0;

        pkt->pdu = *pdu;
        pkt->header = *header;
        pkt->var_bind = *var_bind;

        char buf[256];

        int req_size = snmp_packet_pack(pkt, buf);

        int sockfd;
        int rv;
        struct addrinfo hints, *servinfo, *p2;
        memset(&hints, 0, sizeof hints);
        hints.ai_family = AF_INET; // set to AF_INET to use IPv4
        hints.ai_socktype = SOCK_DGRAM;
        if ((rv = getaddrinfo(agent, port, &hints, &servinfo)) != 0)
        {
            fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
            return 1;
        }

        for (p2 = servinfo; p2 != NULL; p2 = p2->ai_next)
        {
            if ((sockfd = socket(p2->ai_family, p2->ai_socktype,
                                 p2->ai_protocol)) == -1)
            {
                perror("socket");
                continue;
            }

            break;
        }

        if (p2 == NULL)
        {
            fprintf(stderr, "failed to create socket\n");
            return 2;
        }

        int numbytes;
        if ((numbytes = sendto(sockfd, buf, req_size, 0,
                               p2->ai_addr, p2->ai_addrlen)) == -1)
        {
            perror("sendto");
            exit(1);
        }

        struct handler_agrs *h_a = calloc(1, sizeof(struct handler_agrs));
        h_a->sock = sockfd;
        h_a->p2 = p2;

        pthread_t handler;
        int res = pthread_create(&handler, NULL, response_handler, (void *)h_a);
        if (res)
        {
            printf("Error while creating new thread\n");
        }

        freeaddrinfo(servinfo);

        id++;
        free(p2);
    }

    return 0;
}