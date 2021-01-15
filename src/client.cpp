#include <cstdio>
#include <cstdlib>
#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <cstring>
#include <iostream>
#include <string>
#include <termios.h>
using namespace std;
typedef unsigned char uchar;
termios initial_settings, new_settings;
typedef struct{
    char* id; // Идентификатор ошибки
    char* proj; // Проект
    char* text; // Текст
    char* dev; // Разработчик
} bug;
bool is_developer = false;
string name;
//Функция закрытия клиента
void stopClient(int socket){
    tcsetattr(fileno(stdin), TCSANOW, &initial_settings);
    shutdown(socket,SHUT_RDWR);
    close(socket);
    exit(1);
}

//Чтение части пакета
int partRead(int socket, char** buffer){
    int length = 0;
    int n;
    n = recv(socket, &length, sizeof(short), MSG_WAITALL);
    if (n <= 0) {
        perror("ERROR reading from socket");
        stopClient(socket);
    }
    length = ntohs(length);
    *buffer = new char[length+1];
    bzero(*buffer,length+1);
    if (length != 0) n = recv(socket, *buffer, length, MSG_WAITALL);
    if (n <= 0) {
        perror("ERROR reading from socket");
        stopClient(socket);
    }
    return 1;
}

//Отправка части пакета
void writePart(int socket, const char *message){
    int n;
    short length = strlen(message);
    length = htons(length);
    n = send(socket, &length, sizeof(short), 0);
    if (n < 0) {
        perror("ERROR writing to socket");
        stopClient(socket);
    }
    length = ntohs(length);
    n = send(socket, message, length, 0);
    if (n < 0) {
        perror("ERROR writing to socket");
        stopClient(socket);
    }
}
void sendFlag(int socket, char flag) {
    int n;
    //Отправили флаг
    n = send(socket, &flag, sizeof(char),0);
    if (n <= 0) {
        perror("ERROR writing to socket");
        stopClient(socket);
    }
}
//Отправка запроса на сервер
void sendRequestToServer(int socket, char flag){

    int n;
    switch(flag){
        case 1: {
            char prof;
            printf("Введите имя:\n");
            cin >> name;
            printf("Тестер или разработчик?(t/r)\n");
            cin >> prof;
            is_developer = prof == 'r';
            prof = is_developer ? 2 : 1;
            sendFlag(socket, flag);
            n = send(socket, &prof, sizeof(char), 0);
            writePart(socket, name.c_str());
            if (n <= 0) {
                perror("ERROR writing to socket");
                stopClient(socket);
            }
            break;
        }
        case 2: {
            char fixed;
            printf("Получение ошибок\nИсправленные?(y/n): ");
            cin >> fixed;
            fixed = fixed == 'y'? 1 : 0;
            sendFlag(socket, flag);
            n = send(socket,&fixed, sizeof(char), 0);
            if (n <= 0) {
                perror("ERROR writing to socket");
                stopClient(socket);
            }
            break;
        }
        case 3: {
            string id,proj,text,dev;
            printf("Идентификатор ошибки: ");
            cin >> id;
            printf("Проект: ");
            cin >> proj;
            printf("Текст: ");
            cin >> text;
            printf("Разработчик: ");
            cin >> dev;
            sendFlag(socket, flag);
            writePart(socket, id.c_str());
            writePart(socket, proj.c_str());
            writePart(socket, text.c_str());
            writePart(socket, dev.c_str());
            break;
        }
        case 4: {
            int id;
            char approve;
            printf("Подтверждение исправления\nID: ");
            cin >> id;
            printf("Подтверждаете?(y/n): ");
            cin >> approve;
            approve = approve == 'y' ? 1 : 0;
            id = htonl(id);
            sendFlag(socket, flag);
            n = send(socket, &id, sizeof(int), 0);
            if (n <= 0) {
                perror("ERROR writing to socket");
                stopClient(socket);
            }
            n = send(socket, &approve, sizeof(char), 0);
            if (n <= 0) {
                perror("ERROR writing to socket");
                stopClient(socket);
            }
            break;
        }
        case 5: {
            printf("Выдача ошибок для разработчика\n");
            sendFlag(socket, flag);
            break;
        }
        case 6: {
            int id;
            printf("Исправление ошибки\nВведите id ошибки: ");
            cin >> id;
            sendFlag(socket, flag);
            id = htonl(id);
            send(socket, &id, sizeof(int), 0);
            break;
        }
    }

}
void bugPrint(bug* b, int id) {
    printf("ID: %d\n", id);
    printf("In project: %s\n", b->proj);
    printf("%s wrote some bug\n", b->dev);
    printf("Bug identifier: %s\n", b->id);
    printf("Bug description: %s\n", b->text);
}
//Получаем ответ от сервера
void getResponseFromServer(int socket){
    uchar flag = 0;
    int n;
    char *buff;
    //Получаем значение флага
    n = recv(socket, &flag, sizeof(char), MSG_WAITALL);
    printf("%#02x\n",flag);
    if (n <= 0 ) {
        perror("ERROR reading from socket\n");
        stopClient(socket);
    }
    switch(flag){
        case 0x81: {
            printf("Успех\n");
            break;
        }
        case 0x82: {
            short cnt = 0;
            n = recv(socket, &cnt, sizeof(short), MSG_WAITALL);
            cnt = ntohs(cnt);
            if (n < 0) {
                perror("ERROR writing to socket");
                stopClient(socket);
            }
            for (int i = 0; i < cnt; i++){
                int id;
                n = recv(socket, &id, sizeof(int), MSG_WAITALL);
                if (n < 0) {
                    perror("ERROR writing to socket");
                    stopClient(socket);
                }
                bug b;
                id = ntohl(id);
                partRead(socket, &buff);
                b.id = buff;
                partRead(socket, &buff);
                b.proj = buff;
                partRead(socket, &buff);
                b.text = buff;
                partRead(socket, &buff);
                b.dev = buff;
                bugPrint(&b, id);
                free(b.id);
                free(b.proj);
                free(b.text);
                free(b.dev);
            }
            break;

        }
        case 0xC1: {
            printf("Ошибка авторизации\n");
            break;
        }
        case 0xC2: {
            printf("Ошибка получения\n");
            break;
        }
        case 0xC3:{
            printf("Ошибка создания\n");
			break;
        }
        case 0xC4: {
            printf("Ошибка исправления\n");
            break;
        }
    }

}

[[noreturn]] void* reader(void* arg) {
    int socket = *(int*)arg;
    while(true) {
        getResponseFromServer(socket);
    }
}
//argv[2] - host; argv[3] - port
int main(int argc, char *argv[]) {

    int sock_fd;
    uint16_t port_no;
    struct sockaddr_in serv_addr{};
    struct hostent *server;

    //Контролирует нажатую кнопку в терминале
    char pressButton;
    //Начальное состояние консоли
    tcgetattr(fileno(stdin), &initial_settings);

    //Проверка на корректность введенных данных
    if (argc < 2) {
        fprintf(stderr, "usage %s hostname port\n", argv[0]);
        exit(0);
    }

    //Инициализция номера порта
    char* end;
    port_no = (uint16_t) strtol(argv[2], &end,10);
    /*Создание сокета*/
    sock_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (sock_fd < 0) {
        perror("Ошибка при открытии сокета");
        stopClient(sock_fd);
    }

    printf("Здравствуй! Добро пожаловать в систему bug tracking\n");
    
    //Инициализируем соединение с сервером
    server = gethostbyname(argv[1]);
    //Проверяем что хост существует и корректный
    if (server == nullptr) {
        fprintf(stderr, "ERROR, no such host\n");
        exit(0);
    }


    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    bcopy(server->h_addr, (char *) &serv_addr.sin_addr.s_addr, (size_t) server->h_length);
    serv_addr.sin_port = htons(port_no);

    /* Подключаемся к серверу */
    if (connect(sock_fd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        perror("ERROR connecting");
        stopClient(sock_fd);
    }
    pthread_t rTh;
    pthread_create(&rTh, nullptr, reader, &sock_fd);

    printf("Для вызова меню используй клавишу m\nДля тогого чтобы выйти из программы нажмите q\n");
    //Работа клиента
    while(true){

        new_settings = initial_settings;
        new_settings.c_lflag &= ~ICANON;
        new_settings.c_lflag &= ~ECHO;
        new_settings.c_cc[VMIN] = 0;
        new_settings.c_cc[VTIME] = 0;
        tcsetattr(fileno(stdin), TCSANOW, &new_settings);
        //Ожидаем нажатие клавиши
        read(0, &pressButton, 1);

        //Проверка нажатой клавиши
        if(pressButton == 'm'){

            printf("-1 - Авторизация\n");
            printf("-2 - Получение ошибок\n");
            printf("-3 - Посылка ошибки\n");
            printf("-4 - Подтверждение исправления\n");
            printf("-5 - Выдача ошибок разработчику\n");
            printf("-6 - Исправление\n");
            printf("-q - Выйти\n");
        }
        char flag;
        if(pressButton == '1' && name.empty()){
            tcsetattr(fileno(stdin), TCSANOW, &initial_settings);
            flag = 1;
            sendRequestToServer(sock_fd, flag);
        }

        if(pressButton == '2' && !is_developer){
            tcsetattr(fileno(stdin), TCSANOW, &initial_settings);
            flag = 2;
            sendRequestToServer(sock_fd, flag);
        }

        if(pressButton == '3' && !is_developer){
            tcsetattr(fileno(stdin), TCSANOW, &initial_settings);
            flag = 3;
            sendRequestToServer(sock_fd, flag);
        }

        if(pressButton == '4' && !is_developer) {
            tcsetattr(fileno(stdin), TCSANOW, &initial_settings);
            flag = 4;
            sendRequestToServer(sock_fd, flag);
        }

        if(pressButton == '5' && is_developer){
            tcsetattr(fileno(stdin), TCSANOW, &initial_settings);
            flag = 5;
            sendRequestToServer(sock_fd, flag);
        }
        if(pressButton == '6' && is_developer){
            tcsetattr(fileno(stdin), TCSANOW, &initial_settings);
            flag = 6;
            sendRequestToServer(sock_fd, flag);
        }
        if(pressButton == 'q'){
            tcsetattr(fileno(stdin), TCSANOW, &initial_settings);
            stopClient(sock_fd);
            break;
        }
        pressButton = 0;
    }

    return 0;
}