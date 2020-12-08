#include <stdio.h>
#include <string.h> //strlen
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>    //close
#include <arpa/inet.h> //close
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/time.h> //FD_SET, FD_ISSET, FD_ZERO macros
#include <time.h>
#include <fcntl.h>

#define TRUE 1
#define FALSE 0
#define PORT 8888
#define MSG_LIM 2000

static int current_clients = 0;

enum opcode
{
    MSG,
    ERROR
};

typedef union
{

    uint16_t opcode;

    struct
    {
        uint16_t opcode; //MSG
        uint8_t hours;
        uint8_t minutes;
        uint8_t username[16];
        uint8_t message[MSG_LIM];
    } message;

    struct
    {
        uint16_t opcode; // ERROR
        uint8_t error_string[512];
    } error;

} chat_packet;

int main(int argc, char *argv[])
{
    int opt = TRUE;
    int master_socket, addrlen, new_socket, client_socket[256],
        max_clients = 256, activity, i, valread, sd;
    int max_sd;
    struct sockaddr_in address;

    //set of socket descriptors
    fd_set readfds;

    chat_packet packet;

    //initialise all client_socket[] to 0 so not checked
    for (i = 0; i < max_clients; i++)
    {
        client_socket[i] = 0;
    }

    //create a master socket
    if ((master_socket = socket(AF_INET, SOCK_STREAM, 0)) == 0)
    {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }

    //set master socket to allow multiple connections ,
    //this is just a good habit, it will work without this
    if (setsockopt(master_socket, SOL_SOCKET, SO_REUSEADDR, (char *)&opt,
                   sizeof(opt)) < 0)
    {
        perror("setsockopt");
        exit(EXIT_FAILURE);
    }

    //type of socket created
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = PORT;

    //bind the socket to localhost port 8888
    if (bind(master_socket, (struct sockaddr *)&address, sizeof(address)) < 0)
    {
        perror("bind failed");
        exit(EXIT_FAILURE);
    }
    printf("Listener on port %d \n", PORT);

    //try to specify maximum of 3 pending connections for the master socket
    if (listen(master_socket, 3) < 0)
    {
        perror("listen");
        exit(EXIT_FAILURE);
    }
    puts("Waiting for connections ...");

    addrlen = sizeof(address);

    fcntl(new_socket, F_SETFL, O_NONBLOCK);
    fcntl(master_socket, F_SETFL, O_NONBLOCK);

    while (TRUE)
    {
        //printf("Hello\n");
        if ((new_socket = accept(master_socket,
                                 (struct sockaddr *)&address, (socklen_t *)&addrlen)) >= 0)
        {
            //inform user of socket number - used in send and receive commands
            printf("New connection , socket fd is %d , ip is : %s , port : %d \n", new_socket, inet_ntoa(address.sin_addr), ntohs(address.sin_port));

            fcntl(new_socket, F_SETFL, O_NONBLOCK);
            //add new socket to array of sockets
            for (i = 0; i < max_clients; i++)
            {
                //if position is empty
                if (client_socket[i] == 0)
                {
                    client_socket[i] = new_socket;
                    printf("Adding to list of sockets as %d\n", i);
                    current_clients++;
                    break;
                }
            }
        }

        for (i = 0; i < max_clients; i++)
        {
            //printf("Hello in for\n");
            sd = client_socket[i];

            if (sd != 0)
            {
                valread = recv(sd, &packet, sizeof(chat_packet), 0);
                //Check if it was for closing , and also read the
                //incoming message
                if (valread == 0)
                {
                    //Somebody disconnected , get his details and print
                    getpeername(sd, (struct sockaddr *)&address,
                                (socklen_t *)&addrlen);
                    printf("Host disconnected , ip %s , port %d \n",
                           inet_ntoa(address.sin_addr), ntohs(address.sin_port));

                    //Close the socket and mark as 0 in list for reuse
                    close(sd);
                    client_socket[i] = 0;
                    current_clients--;
                }

                //Echo back the message that came in
                else if (valread > 0)
                {
                    time_t T = time(NULL);
                    struct tm tm = *localtime(&T);
                    packet.message.hours = tm.tm_hour;
                    packet.message.minutes = tm.tm_min;
                    if (packet.opcode == MSG)
                    {
                        for (int i = 0; i < max_clients; i++)
                        {
                            if (client_socket[i] != 0)
                            {
                                send(client_socket[i], &packet, sizeof(packet), 0);
                            }
                        }
                    }
                    if (packet.opcode == ERROR)
                    {
                        getpeername(sd, (struct sockaddr *)&address,
                                    (socklen_t *)&addrlen);
                        printf("Host sent error , ip %s , port %d \n",
                               inet_ntoa(address.sin_addr), ntohs(address.sin_port));
                        printf("Error message: %s", packet.error.error_string);
                    }
                }
            }
        }


    }
    return 0;
}