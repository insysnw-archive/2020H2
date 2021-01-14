#include <cstdio>
#include <cstdlib>
#include <netinet/in.h>
#include <unistd.h>
#include <cstring>
#include <pthread.h>
#include <csignal>
#include <map>
#include <mutex>
#include <set>
using std::set;
using std::map;
using std::mutex;

typedef unsigned char uchar;
#define maxClients 10
#define port 8000
typedef struct{
    char *head; // Тема
    char *message; // Текст сообщения
    char *sender; // Отправитель
    char *dest; // Получатель
} mail;
map<int,mail> mails;
//Стартовый id
int nextId = 1;
//Определение мьютекса
mutex m;
//Создаем сокет прослушивающий входящих клиентов
int sockfd;

//Массив клиентов
set<int> clients;

//Функция закрытие клиента
void closeClient(int socket){
    m.lock();
    close(socket);
    clients.erase(socket);
    printf("Клиент с скоетом %d покинул нас\n", socket);
    m.unlock();
    pthread_exit(nullptr);
}

//Функция закрытия сервера 
void closeServer(){
    //Отключить всех клиентов
    m.lock();
    for (auto client: clients) {
        close(client);
    }
    clients.clear();
    m.unlock();
    close(sockfd);
    exit(1);

}

//Чтение части пакета
int partRead(int socket, char** buffer){
    int length = 0;
    int n;
    n = recv(socket, &length, sizeof(short), MSG_WAITALL);
    if (n <= 0) {
        perror("ERROR reading from socket");
        closeClient(socket);
    }
    length = ntohs(length);
    *buffer = new char[length+1];
    bzero(*buffer,length+1);
    n = recv(socket, *buffer, length, MSG_WAITALL);
    if (n <= 0) {
        perror("ERROR reading from socket");
        closeClient(socket);
    }
    return 1;
}

uchar receive(int socket,int flag, int* id, char **email_addr, char **head, char** message, char** dest_addr){
    int n;
    switch(flag){
        case 1: {
            printf("Авторизация\n");
            partRead(socket, email_addr);
            return 0x81;
        }
        case 2: {
            printf("Запрос состояния\n");
            return 0x82;
        }
        case 3: {
            printf("Запрос на отправку сообщения\n");
            partRead(socket, dest_addr);
            partRead(socket, head);
            partRead(socket, message);
            printf("Отправка сообщения от <%s>\n", *email_addr);
            return 0x81;
        }
        case 4: {
            printf("Запрос удаление сообщения\n");
            int res = recv(socket, id, sizeof(int), MSG_WAITALL);
            if (res <= 0) {
                perror("ERROR reading from socket");
                closeClient(socket);
            }
            *id = ntohl(*id);
            return 0x81;
        }
        case 5: {
            printf("Запрос на получение сообщения\n");
            int res = recv(socket, id, sizeof(int), MSG_WAITALL);
            if (res <= 0) {
                perror("ERROR reading from socket");
                closeClient(socket);
            }
            *id = ntohl(*id);
            return 0x83;

        }
        default: {
            perror("Unknown request\n");
        }
    }
    return 0xFF;
}

//Отправка части пакета
void writePart(int socket, const char *message){
    int n;
    short length = strlen(message);
    length = htons(length);
    n = send(socket, &length, sizeof(short), 0);
    if (n < 0) {
        perror("ERROR writing to socket");
        closeClient(socket);
    }
    length = ntohs(length);
    n = send(socket, message, length, 0);
    if (n < 0) {
        perror("ERROR writing to socket");
        closeClient(socket);
    }
}

void response(int socket, int flag, void* data){

    switch(flag){
        case 0x81: {
            int n;
            //Отправили флаг
            n = send(socket, &flag, sizeof(char), 0);
            if (n < 0) {
                perror("ERROR writing to socket");
                closeClient(socket);
            }
            break;
        }
        case 0x82: {
            int n;
            //Отправили флаг
            map<int,mail> ans = *(map<int,mail>*)data;
            n = send(socket, &flag, sizeof(char), 0);
            if (n < 0) {
                perror("ERROR writing to socket");
                closeClient(socket);
            }

            //Отправили количество сообщений
            short size = ans.size();
            size = htons(size);
            n = send(socket, &size, sizeof(short), 0);
            if (n < 0) {
                perror("ERROR writing to socket");
                closeClient(socket);
            }
            for (auto entry: ans){
                int id = entry.first;
                id = htonl(id);
                n = send(socket, &id, sizeof(int), 0);
                if (n < 0) {
                    perror("ERROR writing to socket");
                    closeClient(socket);
                }
                writePart(socket, entry.second.sender);
                writePart(socket, entry.second.head);
            }
            break;
        }
        case 0x83: {
            int n;
            mail msg = *(mail*)data;
            //Отправили флаг
            n = send(socket, &flag, sizeof(char), 0);
            if (n < 0) {
                perror("ERROR writing to socket");
                closeClient(socket);
            }
            writePart(socket, msg.sender);
            writePart(socket, msg.head);
            writePart(socket, msg.message);
            break;
        }
        case 0xC1: {
            int n;
            //Отправили флаг
            n = send(socket, &flag, sizeof(char), 0);
            if (n < 0) {
                perror("ERROR writing to socket");
                closeClient(socket);
            }
            break;

        }
        case 0xC2: {
            int n;
            //Отправили флаг
            n = send(socket, &flag, sizeof(char), 0);
            if (n <= 0) {
                perror("ERROR writing to socket");
                closeClient(socket);
            }
            break;
        }
        case 0xC3: {
            int n;
            //Отправили флаг
            n = send(socket, &flag, sizeof(char), 0);
            if (n <= 0) {
                perror("ERROR writing to socket");
                closeClient(socket);
            }
			break;
        }
		case 0xC4: {
			int n;
            n = send(socket, &flag, sizeof(char), 0);
            if (n <= 0) {
                perror("ERROR writing to socket");
                closeClient(socket);
            }
			break;
		}
    }

}
//Обработчик потока клиента
[[noreturn]] void* clientWorks(void* client){
    
    //Инициализация
    int socket = *(int*) client;
    char* dest = nullptr;
    char* message = nullptr;
    char* head = nullptr;
    char* email = nullptr;
    int flag = 0;
    int id = 0;


    //Слушаем запросы от клиента и даем ему ответы
    while(1){
        //Получаем значение флага
        int n = recv(socket, &flag, sizeof(char), MSG_WAITALL);
        if (n <= 0) {
            perror("ERROR reading from socket\n");
            closeClient(socket);
        }
        int res = 0;
        switch(flag){
            case 1: {
                if (email != nullptr) res = 0xC1;
                else {
                    res = receive(socket, flag, nullptr, &email,
                            nullptr, nullptr, nullptr);
                }
                response(socket, res, nullptr);
                break;
            }
            case 2: {
                map<int, mail> ans;
                if (email == nullptr) res = 0xC1;
                else {
                    res = receive(socket, flag, nullptr, nullptr,
                                  nullptr, nullptr, nullptr);
                    for (auto entry: mails) {
                        if (strcmp(entry.second.dest,email) == 0) {
                            ans[entry.first] = entry.second;
                        }
                    }
                }
                response(socket, res, &ans);
                break;
            }
            case 3: {
                if (email == nullptr) res = 0xC2;
                else {
                    res = receive(socket, flag, nullptr, &email,
                                  &head, &message, &dest);
                    mail msg;
                    msg.dest = dest;
                    msg.head = head;
                    msg.sender = email;
                    msg.message = message;
                    mails[nextId] = msg;
                    nextId++;
                }
                response(socket, res, nullptr);
                break;
            }
            case 4: {
                if (email == nullptr) res = 0xC3;
                else {
                    res = receive(socket, flag, &id, nullptr,
                                  nullptr, nullptr, nullptr);
                    if (mails.find(id) == mails.end()) res = 0xC3;
                    else {
                        mail msg = mails[id];
                        if (strcmp(msg.dest, email) == 0) {
                            mails.erase(id);
                        } else {
                            res = 0xC3;
                        }
                    }
                }
                response(socket, res, nullptr); //--Данные записаны
                printf("Ответ клиенту отправлен\n");

                break;
            }
            case 5: {
                mail msg;
                if (email == nullptr) res = 0xC4;
                else {
                    res = receive(socket, flag, &id, nullptr,
                                  nullptr, nullptr, nullptr);
                    if (mails.find(id) == mails.end() || strcmp(mails[id].dest, email) != 0) res = 0xC4;
                    else msg = mails[id];
                }
                response(socket, res, &msg);
                break;
            }
        }   
        
        flag = 0;
    }
}   

//Обработка сигнала выхода от пользователя
void signalExit(int sig){
    closeServer();
}

int main(int argc, char *argv[]) {
    
    int newsockfd;
    uint16_t portno;
    unsigned int clilen;
    struct sockaddr_in serv_addr, cli_addr;
    ssize_t n;
    signal(SIGINT, signalExit);
    //Идентификатор потока
    pthread_t clientTid;
    /* Сокет для прослушивания других клиентов */
    sockfd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (sockfd < 0) {
        perror("ERROR opening socket");
    }

    /*Инициализируем сервер*/
    bzero((char *) &serv_addr, sizeof(serv_addr));
    portno = port;

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(portno);

    /* Now bind the host address using bind() call.*/
    if (bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        perror("ERROR on binding");
        exit(1);
    }

    /*Слушаем клиентов */
    printf("Сервер запущен. Готов слушать\n");
    
    listen(sockfd, maxClients);
    clilen = sizeof(cli_addr);

    //Работа сервера
    while (1){
        /* Сокет для приёма новых клиентов */
        newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);
        if (newsockfd < 0) {
            perror("ERROR on accept");
            exit(1);
        }
        else{
            m.lock();
            //Добавление нового клиента в массив клиентов
            clients.insert(newsockfd);
            m.unlock();
            //Создаем поток для клиента
            pthread_create(&clientTid, nullptr, clientWorks, &newsockfd);
        }
 
    }
}