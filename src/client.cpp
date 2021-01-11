#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <pthread.h>


#define DEF_PORT 8888
#define DEF_IP "127.0.0.1"
int readFix(int sock, char** buf, char** name, long* time, int flags);
int sendFix(int sock, char* buf, char* name, int flags);
void* reader(void* arg) {
    for (;;) {
        int sock = *(int *) arg;
        char* buf = nullptr;
        char* name = nullptr;
        time_t time;
        int res = readFix(sock, &buf, &name, &time, 0);
        if (res <= 0) {
            perror("Error while receiving:");
            exit(1);
        }
        time -= timezone;
        printf("%s\t%10s:\t%s\n", ctime(&time),name,buf);
        free(buf);
        free(name);
    }
}
int main( int argc, char** argv) {
    tzset();
    char* addr;
    int port;
    char* readbuf;
    if(argc <3) {
        printf("Using default port %d\n",DEF_PORT);
        port = DEF_PORT;
    } else
        port = atoi(argv[2]);
    if(argc < 2) {
        printf("Using default addr %s\n",DEF_IP);
        addr = DEF_IP;
    } else
        addr = argv[1];
    // создаем сокет
    struct sockaddr_in peer;
    peer.sin_family = AF_INET;
    peer.sin_port = htons( port );
    peer.sin_addr.s_addr = inet_addr( addr );
    int sock = socket( AF_INET, SOCK_STREAM, 0 );
    if ( sock < 0 ){
        perror( "Can't create socket\n" );
        exit( 1 );
    }
    // присоединяемся к серверу
    int res = connect( sock, ( struct sockaddr * )&peer, sizeof(peer ) );
    if (res) {
        perror( "Can't connect to server:" );
        exit( 1 );
    }
    // основной цикл программы
    char buf[100];
    char name[20];
    printf("Enter your name\n");
    bzero(name, 20);
    fgets(name, 20, stdin);
    name[strlen(name)-1] = '\0';
    pthread_t thr;
    res = pthread_create(&thr,NULL,reader,(void*)&sock);
    if (res) {
        printf("Error while creating new thread\n");
    }
    for(;;) {
        printf("Input request (empty to exit)\n");
        bzero(buf,100);
        fgets(buf, 100, stdin);
        buf[strlen(buf)-1] = '\0';
        if(strlen(buf) == 0) {
            printf("Bye-bye\n");
            return 0;
        }
        res = sendFix(sock, buf, name, 0);
        if ( res <= 0 ) {
            perror( "Error while sending:" );
            exit( 1 );
        }

    }
}
int readFix(int sock, char** buf, char** name, long* time, int flags) {
    // читаем "заголовок" - сколько байт составляет наше сообщение
    unsigned msgLength = 0;
    unsigned nameLength = 0;
    int res=recv(sock,&msgLength,sizeof(unsigned),flags|MSG_WAITALL);
    if (res <= 0)return res;
    // читаем само сообщение
    *buf = new char[msgLength+1];
    bzero(*buf, msgLength+1);
    res = recv(sock, *buf, msgLength, flags | MSG_WAITALL);
    if (res <= 0) return res;
    res = recv(sock, &nameLength, sizeof(unsigned), flags | MSG_WAITALL);
    if (res <= 0) return res;
    *name = new char[nameLength+1];
    bzero(*name, nameLength+1);
    res = recv(sock, *name, nameLength, flags | MSG_WAITALL);
    if (res <= 0) return res;
    res = recv(sock, time, sizeof(long), flags | MSG_WAITALL);
    if (res <= 0) return res;
    return 1;
}
int sendFix(int sock, char* buf, char* name, int flags) {
    // число байт в сообщении
    unsigned msgLength = strlen(buf);
    unsigned nameLength = strlen(name);
    int res = send(sock, &msgLength, sizeof(unsigned), flags );
    if (res <= 0) return res;
    res = send(sock, buf, msgLength, flags);
    if (res <= 0) return res;
    res = send(sock, &nameLength, sizeof(unsigned), flags);
    if (res <= 0) return res;
    res = send(sock, name, nameLength, flags);
    if (res <= 0) return res;
    return 1;
}
