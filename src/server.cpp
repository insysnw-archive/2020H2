#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <pthread.h>
#include <set>
#define DEF_PORT 8888
#define DEF_IP "127.0.0.1"
std::set<int> clients;
// обработка одного клиента
int readFix(int sock, char** msg, char** name, long* time, int flags);
int sendFix(int sock, char* buf, char* name, long* time, int flags);
void* clientHandler(void* args) {
    int sock = *(int*)args;
    char* buf = nullptr;
    char* name = nullptr;
    int res = 0;
    for(;;) {
        long time;
        res = readFix(sock, &buf,&name, &time, 0);
        if ( res <= 0 ) {
            perror( "Can't recv data from client, ending thread\n" );
            clients.erase(sock);
            pthread_exit(nullptr);
        }
        printf("%s\t%10s:%s\n",ctime(&time),name,buf);
        for (auto it = clients.begin(); it != clients.end(); it++) {
            res = sendFix(*it, buf,name,&time, 0);
            if (res <= 0) {
                perror("send call failed");
                clients.erase(sock);
                pthread_exit(nullptr);
            }
        }
        free(name);
        free(buf);
    }
}
void* main_cycle(void* args) {
    int listener = *(int*)args;
    for(;;) {
        int client = accept(listener, nullptr, nullptr );
        pthread_t thrd;
        int res = pthread_create(&thrd, nullptr, clientHandler, (void*)(&client));
        if (res){
            printf("Error while creating new thread\n");
            break;
        }
        clients.insert(client);
    }
}
int main( int argc, char** argv) {
    int port = 0;
    if(argc < 2) {
    printf("Using default port %d\n",DEF_PORT);
    port = DEF_PORT;
    } else
        port = atoi(argv[1]);
    struct sockaddr_in listenerInfo;
    listenerInfo.sin_family = AF_INET;
    listenerInfo.sin_port = htons( port );
    listenerInfo.sin_addr.s_addr = htonl( INADDR_ANY );
    int listener = socket(AF_INET, SOCK_STREAM, 0 );
    if ( listener < 0 ) {
        perror( "Can't create socket to listen: " );
        exit(1);
    }
    int res = bind(listener,(struct sockaddr *)&listenerInfo, sizeof(listenerInfo));
    if ( res < 0 ) {
        perror( "Can't bind socket" );
        exit( 1 );
    }
    // слушаем входящие соединения
    res = listen(listener,5);
    if (res) {
        perror("Error while listening:");
        exit(1);
    }
    // основной цикл работы
    pthread_t cycle;
    res = pthread_create(&cycle, nullptr, main_cycle, (void*)(&listener));
    if (res){
        printf("Error while creating new thread\n");
    }
    char input;
    for(;;) {
        input = (char)getchar();
        if (input == 'q') {
            exit(0);
        }
    }
}
int readFix(int sock, char** msg, char** name, long* time, int flags) {
    // читаем "заголовок" - сколько байт составляет наше сообщение
    unsigned msgLength = 0;
    unsigned nameLength = 0;
    struct tm *t;
    int res=recv(sock,&msgLength,sizeof(unsigned),flags|MSG_WAITALL );
    if (res <= 0)return res;
    // читаем само сообщение
    *msg = new char[msgLength+1];
    bzero(*msg, msgLength+1);
    res = recv(sock, *msg, msgLength, flags | MSG_WAITALL);
    if (res <= 0) return res;
    res = recv(sock,&nameLength, sizeof(unsigned), flags|MSG_WAITALL);
    if (res <= 0) return res;
    *name= new char[nameLength+1];
    bzero(*name, nameLength+1);
    res = recv(sock, *name, nameLength, flags|MSG_WAITALL);
    if (res <= 0) return res;
    *time = ::time(nullptr);
    t = gmtime(time);
    *time = mktime(t);
    return 1;
}
int sendFix(int sock, char* buf, char* name, long* time, int flags) {
    // шлем число байт в сообщении
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
    res = send(sock, time, sizeof(long), flags);
    if (res <= 0) return res;
    return 1;
}
