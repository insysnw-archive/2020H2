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

struct test
{
    char *name;
    char *questions[4];
    char *right_answers[4];
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
    int is_logged = 0;
    int question_number = 0;
    int result[4];
    result[0] = 0;
    result[1] = 0;
    result[2] = 0;
    result[3] = 0;

    int current_test_number = 0;

    struct test *tests[3];
    int tests_size = 3;

    struct test *test1 = calloc(1, sizeof(struct test));

    test1->questions[0] = "1+1";
    test1->questions[1] = "1+2";
    test1->questions[2] = "1+3";
    test1->questions[3] = "1+4";

    test1->right_answers[0] = "2";
    test1->right_answers[1] = "3";
    test1->right_answers[2] = "4";
    test1->right_answers[3] = "5";

    char *name1 = "first one";

    test1->name = name1;

    struct test *test2 = calloc(1, sizeof(struct test));

    test2->questions[0] = "1*1";
    test2->questions[1] = "1*2";
    test2->questions[2] = "1*3";
    test2->questions[3] = "1*4";

    test2->right_answers[0] = "1";
    test2->right_answers[1] = "2";
    test2->right_answers[2] = "3";
    test2->right_answers[3] = "4";
    char *name2 = "second one";

    test2->name = name2;

    struct test *test3 = calloc(1, sizeof(struct test));

    test3->questions[0] = "2+1";
    test3->questions[1] = "2+2";
    test3->questions[2] = "2+3";
    test3->questions[3] = "2+4";

    test3->right_answers[0] = "3";
    test3->right_answers[1] = "4";
    test3->right_answers[2] = "5";
    test3->right_answers[3] = "6";
    char *name3 = "third one";

    test3->name = name3;

    tests[0] = test1;
    tests[1] = test2;
    tests[2] = test3;

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
                printf("server: socket %d hung up\n", sock);
            }
            else
            {
                perror("recv");
            }

            close(sock);
            return;
        }

        packet_parse(pkt, buf, res);

        packet_print(pkt);

        struct protocol_packet *back_pkt = calloc(1, sizeof(struct protocol_packet));

        if (pkt->type == REGISTER_REQUEST)
        {
            //Simulating register check
            is_logged = 1;
            back_pkt->type = REGISTER_RESPONSE;
            back_pkt->error_code = 0;
        }
        else
        {
            if (is_logged == 0)
            {
                back_pkt->type = pkt->type;
                back_pkt->error_code = 2;
            }
        }

        if (pkt->type == TEST_NUMBER && is_logged == 1)
        {
            if (pkt->test_number < tests_size && pkt->test_number >= 0)
            {
                current_test_number = pkt->test_number;
                question_number = 0;
                back_pkt->type = QUESTION;
                back_pkt->question_size = strlen(tests[current_test_number]->questions[question_number]);
                back_pkt->question = calloc(back_pkt->question_size, sizeof(char));
                memcpy(back_pkt->question, tests[current_test_number]->questions[question_number], back_pkt->question_size);
                question_number = 1;
            }
            else
            {
                back_pkt->type = pkt->type;
                back_pkt->error_code = 3;
            }
        }

        if (pkt->type == QUESTION_RESPONSE && is_logged == 1)
        {
            if (strcmp(pkt->answer, tests[current_test_number]->right_answers[question_number - 1]) == 0)
            {
                result[question_number - 1] = 1;
            }
            else
            {
                result[question_number - 1] = 0;
            }
            if (question_number > 3)
            {
                back_pkt->type = STRING_ARG;
                char *str = calloc(256, sizeof(char));
                for (int i = 0; i < 4; i++)
                {
                    char t[3];
                    sprintf(t, "%d", result[i]);
                    strcat(str, t);
                    strcat(str, "\n");
                }
                back_pkt->str_arg_size = strlen(str);
                back_pkt->str_arg = calloc(back_pkt->str_arg_size, sizeof(char));
                memcpy(back_pkt->str_arg, str, back_pkt->str_arg_size);
            }
            else
            {
                back_pkt->type = QUESTION;
                back_pkt->question_size = strlen(tests[current_test_number]->questions[question_number]);
                back_pkt->question = calloc(back_pkt->question_size, sizeof(char));
                memcpy(back_pkt->question, tests[current_test_number]->questions[question_number], back_pkt->question_size);
                question_number++;
            }
        }

        if (pkt->type == REQUEST_LAST_RESULT && is_logged == 1)
        {
            back_pkt->type = STRING_ARG;
            char *str = calloc(256, sizeof(char));
            for (int i = 0; i < 4; i++)
            {
                char t[3];
                sprintf(t, "%d", result[i]);
                strcat(str, t);
                strcat(str, "\n");
            }
            back_pkt->str_arg_size = strlen(str);
            back_pkt->str_arg = calloc(back_pkt->str_arg_size, sizeof(char));
            memcpy(back_pkt->str_arg, str, back_pkt->str_arg_size);
        }

        if (pkt->type == REQUEST_ALL_TEST && is_logged == 1)
        {
            back_pkt->type = STRING_ARG;
            char *str = calloc(256, sizeof(char));
            for (int i = 0; i < tests_size; i++)
            {
                char t[3];
                sprintf(t, "%d", i);
                strcat(str, t);
                strcat(str, ".");
                strcat(str, tests[i]->name);
                strcat(str, "\n");
            }
            back_pkt->str_arg_size = strlen(str);
            back_pkt->str_arg = calloc(back_pkt->str_arg_size, sizeof(char));
            memcpy(back_pkt->str_arg, str, back_pkt->str_arg_size);
        }

        packet_print(back_pkt);
        int req_size = packet_pack(back_pkt, buf);

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
            pthread_t clientH;
            int res = pthread_create(&clientH, NULL, clientHandler, (void *)(&newfd));
            if (res)
            {
                printf("Error while creating new thread\n");
            }

            printf("server: new connection from %s on "
                   "socket %d\n",
                   inet_ntop(remoteaddr.ss_family,
                             get_in_addr((struct sockaddr *)&remoteaddr),
                             remoteIP, INET6_ADDRSTRLEN),
                   newfd);
        }
    }

    return 0;
}
