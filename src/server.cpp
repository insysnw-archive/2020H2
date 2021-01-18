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
#include <vector>
#include <string>

using namespace std;

typedef unsigned char uchar;
#define port 53
typedef struct {
    char *head; // Тема
    char *message; // Текст сообщения
    char *sender; // Отправитель
    char *dest; // Получатель
} mail;
map<int, mail> mails;
//Стартовый id
int nextId = 1;
//Определение мьютекса
mutex m;
//Создаем сокет прослушивающий входящих клиентов
int sockfd;

//Массив клиентов
set<int> clients;
uint8_t DNS_PORT = 53;

enum class MessageType : uint8_t {
    QUERY = 0,
    RESPONSE = 1
};

enum class Opcode : uint8_t {
    QUERY = 0,
    IQUERY = 1,
    STATUS = 2
};

enum class Rcode : uint8_t {
    NO_ERROR = 0,
    FORMAT_ERROR = 1,
    SERVER_FAILURE = 2,
    NAME_ERROR = 3,
    NOT_IMPLEMENTED = 4,
    REFUSED = 5
};

enum class QType : uint16_t {
    A = 1,
    MX = 15,
    TXT = 16,
    AAAA = 28
};

enum class QClass : uint16_t {
    IN = 1,
    CS = 2,
    CH = 3,
    HS = 4
};

struct DnsHeader {
    uint16_t id;
    uint16_t flags; // QR, Opcode, AA, TC, RD, RA, RCODE
    uint16_t qdCount;
    uint16_t anCount;
    uint16_t nsCount;
    uint16_t arCount;
};

struct Query {
    string name;
    QType type;
    QClass qClass;
};

struct ResourceRecord {
    std::string name;
    QType type;
    QClass qClass;
    uint32_t ttl;
    std::vector<uint8_t> recordData;
};

//Функция закрытие клиента
void closeClient(int socket) {
    m.lock();
    close(socket);
    clients.erase(socket);
    printf("Клиент с скоетом %d покинул нас\n", socket);
    m.unlock();
    pthread_exit(nullptr);
}

//Функция закрытия сервера 
void closeServer() {
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

DnsHeader header;

MessageType getMessageType() {
    return static_cast<MessageType>((header.flags & 0b1000000000000000u) >> 15u);
}

void setMessageType(MessageType type) {
    if (type == MessageType::RESPONSE) {
        header.flags |= 0x8000;
    } else {
        header.flags &= ~0x8000;
    }
}

Opcode getOpcode() {
    return (Opcode) ((header.flags & 0x7800) >> 11u);
}

void setOpcode(Opcode opcode) {
    header.flags &= ~0x7800;
    header.flags |= (uint16_t) ((uint16_t) opcode << 11u);
}

bool isAuthoritativeAnswer() {
    return header.flags & 0x0400;
}

void setAuthoritativeAnswer(bool value) {
    if (value) {
        header.flags |= 0x0400;
    } else {
        header.flags &= ~0x0400;
    }
}

bool wasTruncated() {
    return header.flags & 0x0200;
}

void setTruncated(bool value) {
    if (value) {
        header.flags |= 0x0200;
    } else {
        header.flags &= ~0x0200;
    }
}

bool isRecursionDesired() {
    return header.flags & 0x0100;
}

void setRecursionDesired(bool value) {
    if (value) {
        header.flags |= 0x0100;
    } else {
        header.flags &= ~0x0100;
    }
}

bool isRecursionAvailable() {
    return header.flags & 0x0080;
}

void setRecursionAvailable(bool value) {
    if (value) {
        header.flags |= 0x0080;
    } else {
        header.flags &= ~0x0080;
    }
}

Rcode getResponseCode() {
    return static_cast<Rcode>(header.flags & 0x000F);
}

void setResponseCode(Rcode code) {
    header.flags &= ~0x000F;
    header.flags |= (uint16_t) (code);
}

uint16_t getQuestionEntriesCount() {
    return header.qdCount;
}

void setQuestionEntriesCount(uint16_t count) {
    header.qdCount = count;
}

uint16_t getResourceRecordsCount() {
    return header.anCount;
}

void setResourceRecordsCount(uint16_t count) {
    header.anCount = count;
}

uint16_t getNameServerRRCount() {
    return header.nsCount;
}

void setNameServerRRCount(uint16_t count) {
    header.nsCount = count;
}

uint16_t getAdditionalRRCount() {
    return header.arCount;
}

void setAdditionalRRCount(uint16_t count) {
    header.arCount = count;
}

uint16_t getId() {
    return header.id;
}

void setId(uint16_t id) {
    header.id = id;
}

uchar receive(int socket, int flag) {

    return 0xFF;
}

ResourceRecord response(Query query, DnsHeader hQuery) {
    DnsHeader hResp;
    header.id = htonl(header.id);
    setMessageType(MessageType::RESPONSE);
    setOpcode(Opcode::QUERY);
    setAuthoritativeAnswer(false);
    setTruncated(false);
    setRecursionDesired(false);
    setRecursionAvailable(false);
    setQuestionEntriesCount(0);
    setResourceRecordsCount(1);
    setNameServerRRCount(0);
    setAdditionalRRCount(0);
    setResponseCode(Rcode::NO_ERROR);
    bool rd = (hResp = header, header = hQuery, isRecursionDesired());
    header = hResp;
    setRecursionDesired(rd);
    setRecursionAvailable(true);
    ResourceRecord resp;
    switch (query.type) {
        case QType::A: {
            std::vector<uint8_t> answer{192, 168, 1, 4};
            resp = ResourceRecord{query.name, query.type, query.qClass, 0, answer};
            break;
        }
        case QType::AAAA: {
            std::vector<uint8_t> answer(16, 0);
            answer[12] = 192;
            answer[13] = 168;
            answer[14] = 1;
            answer[15] = 4;
            resp = ResourceRecord{query.name, query.type, query.qClass, 0, answer};
            break;
        }
        case QType::MX: {
            std::vector<uint8_t> answer{
                    0, 1, // Preference
                    2, 'm','y', 4, 'm','a','i','l', 0 // Exchange
            };
            resp = ResourceRecord{query.name, query.type, query.qClass, 0, answer};
            break;
        }
        case QType::TXT: {
            std::vector<uint8_t> answer{
                    11, 'H', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd' // Exchange
            };
            resp = ResourceRecord{query.name, query.type, query.qClass, 0, answer};
            break;
        }
        default:
            throw std::exception();
    }
    return resp;
}
ResourceRecord respErr() {
    setMessageType(MessageType::RESPONSE);
    setOpcode(Opcode::QUERY);
    setAuthoritativeAnswer(false);
    setTruncated(false);
    setRecursionDesired(false);
    setRecursionAvailable(false);
    setResponseCode(Rcode::NOT_IMPLEMENTED);
    setQuestionEntriesCount(0);
    setResourceRecordsCount(0);
    setNameServerRRCount(0);
    setAdditionalRRCount(0);
    ResourceRecord resp;
    return resp;
}

//Обработка сигнала выхода от пользователя
void signalExit(int sig) {
    closeServer();
}

Query q;

int decodeQuery(const uchar *buffer) {
    Query query{};
    int totalLength = 0;
    int length;
    do {
        length = (unsigned char) *buffer++;
        totalLength++;

        for (int i = 0; i < length; i++) {
            char c = *buffer++;
            totalLength++;
            query.name.append(1, c);
        }
        if (length != 0) {
            query.name.append(1, '.');
        }

    } while (length != 0);

    memcpy(&query.type, buffer, sizeof(query.type));
    query.type = (QType) (ntohs((uint16_t) query.type));
    memcpy(&query.qClass, buffer + sizeof(query.type), sizeof(query.qClass));
    query.qClass = (QClass) (ntohs((uint16_t) query.qClass));
    q = query;
    return totalLength + (int) sizeof(query.type) + (int) sizeof(query.qClass);
}

void qMsgInit(const uchar *buffer, long size) {
    if (size < sizeof(header)) {
        std::fprintf(stderr, "Error! Can't parse message\n");
    }
    memcpy(&header, buffer, sizeof(header));
    header.id = ntohs(header.id);
    header.flags = ntohs(header.flags);
    header.qdCount = ntohs(header.qdCount);
    header.anCount = ntohs(header.anCount);
    header.nsCount = ntohs(header.nsCount);
    header.arCount = ntohs(header.arCount);
    decodeQuery(buffer + sizeof(header));
}

int encodeResourceRecord(const ResourceRecord &record, uchar *buffer){
    int start = 0;
    int end;
    uint16_t totalLength = 0;
    while ((end = record.name.find('.', start)) != std::string::npos) {
        *buffer++ = (uint8_t) (end - start); // label length
        totalLength += 1;
        for (int i = start; i < end; i++) {
            *buffer++ = record.name[i]; // label
            totalLength += 1;
        }
        start = end + 1; // skip '.'
    }
    *buffer++ = (uint8_t) (record.name.size() - start);
    totalLength += 1;
    for (int i = start; i < record.name.size(); i++) {
        *buffer++ = record.name[i]; // last label
        totalLength += 1;
    }

    uint16_t type = ntohs((uint16_t)record.type);
    uint16_t qClass = ntohs((uint16_t)record.qClass);
    uint32_t ttl = ntohl(record.ttl);
    *buffer = 0;

    std::memcpy(buffer, &type, sizeof(type));
    std::memcpy(buffer + sizeof(type), &qClass, sizeof(qClass));
    std::memcpy(buffer + sizeof(type) + sizeof(qClass), &ttl, sizeof(ttl));
    uint16_t dataLength = record.recordData.size();
    uint16_t swappedLength = ntohs(dataLength);
    std::memcpy(buffer + sizeof(type) + sizeof(qClass) + sizeof(ttl), &swappedLength, sizeof(dataLength));
    std::memcpy(buffer + sizeof(type) + sizeof(qClass) + sizeof(ttl) + sizeof(dataLength),
                record.recordData.data(), dataLength);
    return totalLength + sizeof(type) + sizeof(qClass) + sizeof(ttl) + sizeof(dataLength) + dataLength;
}

int encode(uchar* buff, ResourceRecord record) {
    DnsHeader tempHeader {}; // to inverse the byte order
    tempHeader.id = htons(header.id);
    tempHeader.flags = htons(header.flags);
    tempHeader.qdCount = htons(header.qdCount);
    tempHeader.anCount = htons(header.anCount);
    tempHeader.nsCount = htons(header.nsCount);
    tempHeader.arCount = htons(header.arCount);
    std::memcpy(buff, &tempHeader, sizeof(tempHeader));
    int totalSize = sizeof(tempHeader);
    if (header.anCount != 0) totalSize += encodeResourceRecord(record, buff + totalSize);
    return totalSize;
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
    sockfd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
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
    uchar buff[512];
    uchar rbuff[512];
    int rsize;
    /*Слушаем клиентов */
    printf("Сервер запущен. Готов слушать\n");
    while (true) {
        struct sockaddr_in caddr{};
        int len = sizeof(caddr);
        int s = recvfrom(sockfd, buff, 512, MSG_WAITALL, (sockaddr *) &caddr, (socklen_t *) &len);
//        caddr.sin_port = ntohl(caddr.sin_port);
        qMsgInit(buff, s);
        printf("Addr: %d\n", ntohl(caddr.sin_addr.s_addr));
        printf("Port: %d\n", ntohs(caddr.sin_port));
        printf("Query message received.\n");
        printf("ID: %d, MsgType: %hhu, RD: %d, RespCode: %hhu, QCnt: %d.\n",
               getId(), getMessageType(),
               isRecursionDesired(), getResponseCode(), getQuestionEntriesCount());

        printf("Name: %s, Type: %hu, Class: %hu\n",
               q.name.c_str(), (uint16_t) (q.type), (uint16_t) (q.qClass));

        try {
            printf("Query supported\n");
            rsize = encode(rbuff,response(q,header));
            printf("Send %d bytes\n", rsize);
            sendto(sockfd, rbuff, rsize, 0, (sockaddr*)&caddr, (socklen_t)sizeof(caddr));
        } catch (const std::exception &exception) {
            encode(rbuff,respErr());
            printf("Query not supported\n");
            sendto(sockfd, rbuff, rsize, 0, (sockaddr*)&caddr, (socklen_t)sizeof(caddr));
        }
    }
}