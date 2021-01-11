#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <pthread.h>
#include <set>
#include <fcntl.h>
#include <algorithm>
#include <zconf.h>
#include <cerrno>

#define DEF_PORT 8888
#define DEF_IP "127.0.0.1"
std::set<int> clients;

// обработка одного клиента
int readFix(int sock, char **msg, char **name, long *time, int flags);

int sendFix(int sock, char *buf, char *name, long *time, int flags);

void *main_cycle(void *args) {
    while (true) {
        int listener = *(int *) args;
        fd_set readset;
        FD_ZERO(&readset);
        FD_SET(listener, &readset);
        for (auto it = clients.begin(); it != clients.end(); it++) {
            FD_SET(*it, &readset);
        }
        timeval timeout;
        timeout.tv_sec = 1;
        timeout.tv_usec = 0;
        int mx = std::max(listener, *std::max_element(clients.begin(), clients.end()));
        int cnt = select(mx + 1, &readset, nullptr, nullptr, &timeout);
        if (cnt == -1) {
            perror("Failed select\n");
            exit(5);
        } else if (cnt == 0) continue;
        if (FD_ISSET(listener, &readset)) {
            int client = accept(listener, nullptr, nullptr);
            if (client <= 0) {
                perror("Failed accept\n");
                exit(5);
            }
            fcntl(client, F_SETFL, O_NONBLOCK);
            clients.insert(client);
        }
        for (auto it = clients.begin(); it != clients.end(); it++) {
            if (FD_ISSET(*it, &readset)) {
                char *buf = nullptr;
                char *name = nullptr;
                int res = 0;
                long time;
                res = readFix(*it, &buf, &name, &time, 0);
                int err = errno; // save off errno, because because the printf statement might reset it
                if (res < 0){
                    if (err == EAGAIN){
                        printf("non-blocking operation returned EAGAIN or EWOULDBLOCK\n");
                        continue;
                    }else{
                        printf("recv returned unrecoverable error(errno=%d)\n", err);
                        break;
                    }
                }
                if (res < 0) {
                    perror("Can't recv data from client\n");
                    close(listener);
                    clients.erase(*it);
                    exit(1);
                } else if (res == 0) {
                    FD_CLR(*it, &readset);
                    close(*it);
                    printf("Disconnected client(%d)\n",*it);
                    continue;
                }
                printf("%s\t%10s:%s\n", ctime(&time), name, buf);
                for (int client : clients) {
                    res = sendFix(client, buf, name, &time, 0);
                    if (res < 0) {
                        perror("send call failed");
                        continue;
                    }
                }
                free(name);
                free(buf);

            }
        }
    }
}

int main(int argc, char **argv) {
    int port = 0;
    if (argc < 2) {
        printf("Using default port %d\n", DEF_PORT);
        port = DEF_PORT;
    } else
        port = atoi(argv[1]);
    struct sockaddr_in listenerInfo;
    listenerInfo.sin_family = AF_INET;
    listenerInfo.sin_port = htons(port);
    listenerInfo.sin_addr.s_addr = htonl(INADDR_ANY);
    int listener = socket(AF_INET, SOCK_STREAM, 0);
    fcntl(listener, F_SETFL, O_NONBLOCK);
    if (listener < 0) {
        perror("Can't create socket to listen: ");
        exit(1);
    }
    int res = bind(listener, (struct sockaddr *) &listenerInfo, sizeof(listenerInfo));
    if (res < 0) {
        perror("Can't bind socket");
        exit(1);
    }
    // слушаем входящие соединения
    res = listen(listener, 5);
    if (res) {
        perror("Error while listening:");
        exit(1);
    }
    // основной цикл работы
    pthread_t cycle;
    res = pthread_create(&cycle, nullptr, main_cycle, (void *) (&listener));
    if (res) {
        printf("Error while creating new thread\n");
    }
    char input;
    for (;;) {
        input = (char) getchar();
        if (input == 'q') {
            exit(0);
        }
    }
}
ssize_t rec(int sock, void* data, size_t n, int flags) {
    int res = 0;
    int err = 0;
    do {
        res = recv(sock, data, n, flags);
        err = errno; // save off errno, because because the printf statement might reset it
        if (res < 0){
            if (err == EAGAIN){
            }else{
                printf("recv returned unrecoverable error(errno=%d)\n", err);
                break;
            }
        }
    } while (err == EAGAIN && res < 0);
    return res;
}

int readFix(int sock, char **msg, char **name, long *time, int flags) {
    // читаем "заголовок" - сколько байт составляет наше сообщение
    unsigned msgLength = 0;
    unsigned nameLength = 0;
    struct tm *t;
    int res = rec(sock, &msgLength, sizeof(unsigned), flags);
    if (res <= 0)return res;
    // читаем само сообщение
    *msg = new char[msgLength + 1];
    bzero(*msg, msgLength + 1);
    res = rec(sock, *msg, msgLength, flags);
    if (res <= 0) return res;
    res = rec(sock, &nameLength, sizeof(unsigned), flags);
    if (res <= 0) return res;
    *name = new char[nameLength + 1];
    bzero(*name, nameLength + 1);
    res = rec(sock, *name, nameLength, flags);
    if (res <= 0) return res;
    *time = ::time(nullptr);
    t = gmtime(time);
    *time = mktime(t);
    return 1;
}
int sendFix(int sock, char *buf, char *name, long *time, int flags) {
    // шлем число байт в сообщении
    unsigned msgLength = strlen(buf);
    unsigned nameLength = strlen(name);
    int res = send(sock, &msgLength, sizeof(unsigned), flags);
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
