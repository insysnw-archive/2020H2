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
#include <time.h>
#include <string.h>

#include "protocol.h"

#define DEF_PORT "8888"
#define DEF_IP "127.0.0.1"

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
    char *addr, *port;
    int numbytes;
    int o;
    char *last_question = calloc(4, sizeof(char));
    memcpy(last_question, "who?", 4);
    int last_question_len;

    port = DEF_PORT;
    addr = DEF_IP;

    while ((o = getopt(argc, argv, "p:a:")) != -1)
    {
        switch (o)
        {
        case 'p':
            port = optarg;
            break;
        case 'a':
            addr = optarg;
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

    int is_login = 0;

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

    char buf[256];

    struct pollfd *pfds = malloc(sizeof *pfds * 2);
    pfds[0].fd = 0; //Ввод пользователя
    pfds[0].events = POLLIN;

    pfds[1].fd = sockfd; //Сервер
    pfds[1].events = POLLIN;

    printf("Connected to server!\nTo login use [login login password] \n To to get last result use [last]\nTo get all test use [list]\nTo answer question use [answer your_answer]\nTo choose test use [test test_number]\n");

    //Основной цикл
    while (1)
    {
        bzero(buf, sizeof(buf));
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

            buf[numbytes - 1] = '\n';

            //Прочитать удалось, анализируем
            int count = 0;
            int flag = 0;
            char *p2 = buf;
            struct protocol_packet *pkt = calloc(1, sizeof(struct protocol_packet));
            //pkt->request = calloc(1, sizeof(struct protocol_request));
            while (1)
            {
                if (*p2 == ' ' || *p2 == '\n')
                {
                    char *t = calloc(count, sizeof(char));
                    memcpy(t, p2 - count, count);
                    if (flag == 0)
                    {
                        if (strcmp(t, "login") == 0)
                        {
                            if (*p2 == '\n')
                            {
                                printf("login needs 2 arguments\n");
                                flag = -1;
                                break;
                            }
                            else
                            {
                                pkt->type = REGISTER_REQUEST;
                                pkt->error_code = 0;
                                flag = 1;
                            }
                        }

                        if (strcmp(t, "test") == 0)
                        {
                            if (*p2 == '\n')
                            {
                                printf("test needs 1 argument\n");
                                flag = -1;
                                break;
                            }
                            else
                            {
                                pkt->type = TEST_NUMBER;
                                pkt->error_code = 0;
                                flag = 2;
                            }
                        }

                        if (strcmp(t, "answer") == 0)
                        {
                            if (*p2 == '\n')
                            {
                                printf("answer needs 1 argument\n");
                                flag = -1;
                                break;
                            }
                            else
                            {
                                pkt->type = QUESTION_RESPONSE;
                                pkt->error_code = 0;
                                flag = 3;
                            }
                        }

                        if (strcmp(t, "last") == 0)
                        {
                            if (*p2 != '\n')
                            {
                                printf("last doesn't take arguments\n");
                                flag = -1;
                                break;
                            }
                            else
                            {
                                pkt->type = REQUEST_LAST_RESULT;
                                pkt->error_code = 0;
                                flag = 10;
                                break;
                            }
                        }

                        if (strcmp(t, "list") == 0)
                        {
                            if (*p2 != '\n')
                            {
                                printf("list doesn't take arguments\n");
                                flag = -1;
                                break;
                            }
                            else
                            {
                                pkt->type = REQUEST_ALL_TEST;
                                pkt->error_code = 0;
                                flag = 10;
                                break;
                            }
                        }

                        if (flag == 0)
                        {
                            break;
                        }
                    }
                    else
                    {
                        if (flag == 1)
                        {
                            if (*p2 == '\n')
                            {
                                printf("login takes 2 arguments\n");
                                flag = -1;
                                break;
                            }
                            else
                            {
                                pkt->login_size = count;
                                pkt->login = calloc(count, sizeof(char));
                                memcpy(pkt->login, t, pkt->login_size);
                                flag = 5;
                                p2++;
                                count = 0;
                                continue;
                            }
                        }
                        if (flag == 2)
                        {
                            if (*p2 != '\n')
                            {
                                printf("too many arguments\n");
                                flag = -1;
                                break;
                            }
                            else
                            {
                                pkt->test_number = (u_int16_t)strtol(t, NULL, 10);
                                flag = 10;
                                break;
                            }
                        }
                        if (flag == 3)
                        {
                            if (*p2 != '\n')
                            {
                                printf("too many arguments\n");
                                flag = -1;
                                break;
                            }
                            else
                            {
                                pkt->question_size = last_question_len;
                                pkt->question = calloc(last_question_len, sizeof(char));
                                memcpy(pkt->question, last_question, last_question_len);
                                pkt->answer_size = count;
                                pkt->answer = calloc(pkt->answer_size, sizeof(char));
                                memcpy(pkt->answer, t, count);
                                flag = 10;
                                break;
                            }
                        }

                        if (flag == 5)
                        {
                            if (*p2 != '\n')
                            {
                                printf("too many arguments\n");
                                flag = -1;
                                break;
                            }
                            else
                            {
                                pkt->password_size = count;
                                pkt->password = calloc(count, sizeof(char));
                                memcpy(pkt->password, t, count);
                                flag = 10;
                                break;
                            }
                        }
                    }
                    count = 0;
                }
                else
                {
                    count++;
                }
                p2++;
            }
            if (flag <= 5)
            {
                continue;
            }

            char buf2[256];
            packet_print(pkt);

            int req_size = packet_pack(pkt, buf2);

            int res = send(pfds[1].fd, buf2, req_size, 0);
            if (res <= 0)
                printf("Failed to send packet\n");
            res = recv(pfds[1].fd, buf2, 256, 0);
            struct protocol_packet *back_pkt = calloc(1, sizeof(struct protocol_packet));
            packet_parse(back_pkt, buf2, res);
            if (back_pkt->error_code != 0)
            {
                printf("Error %d\n", back_pkt->error_code);
            }
            else
            {
                if (back_pkt->type == REGISTER_RESPONSE)
                {
                    printf("Logged succesfully\n");
                }
                if (back_pkt->type == QUESTION)
                {
                    printf("Question: %s\n", back_pkt->question);
                    last_question_len = pkt->question_size;
                    free(last_question);
                    last_question = calloc(last_question_len, sizeof(char));
                    memcpy(last_question, pkt->question, last_question_len);
                }
                if (back_pkt->type == STRING_ARG)
                {
                    printf("%s\n", back_pkt->str_arg);
                }
            }
            free(back_pkt);
        }
        else
        {
            pfds[1].revents = 0;
        }
    }
    return 0;
}