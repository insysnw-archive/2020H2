#include <cstdio>
#include <cstdlib>
#include <netdb.h>
#include <netinet/in.h>
#include <linux/ip.h>
#include <linux/udp.h>
#include <unistd.h>
#include <cstring>
#include <iostream>
#include <string>
#include <libnet.h>

using namespace std;
typedef unsigned char uchar;
int sock;
uchar msg[28+576];
unsigned short csum(unsigned short *buf, int nwords)
{
    unsigned long sum;
    for(sum=0; nwords>0; nwords--)
        sum += *buf++;
    sum = (sum >> 16) + (sum &0xffff);
    sum += (sum >> 16);
    return (unsigned short)(~sum);
}
//Функция закрытия клиента
void stopClient(int socket){
    shutdown(socket,SHUT_RDWR);
    close(socket);
    exit(1);
}
uchar* formMsg(int type) {
    uchar* it = msg;
    *it = 0x01; it++;
    *it = 0x01; it++;
    *it = 0x06; it++;
    *it = 0x00; it++;
    int xid = rand();
    memcpy(it,&xid,4); it+=4;
    bzero(it,20); it+=20;
    struct ifreq ifr;
    struct ifconf ifc;
    char buf[1024];
    int success = 0;
    if (sock == -1) { /* handle error*/ };
    ifc.ifc_len = sizeof(buf);
    ifc.ifc_buf = buf;
    if (ioctl(sock, SIOCGIFCONF, &ifc) == -1) { /* handle error */ }
    struct ifreq* itfreq = ifc.ifc_req;
    const struct ifreq* const end = itfreq + (ifc.ifc_len / sizeof(struct ifreq));
    for (; itfreq != end; ++itfreq) {
        strcpy(ifr.ifr_name, itfreq->ifr_name);
        if (ioctl(sock, SIOCGIFFLAGS, &ifr) == 0) {
            if (! (ifr.ifr_flags & IFF_LOOPBACK)) { // don't count loopback
                if (ioctl(sock, SIOCGIFHWADDR, &ifr) == 0) {
                    success = 1;
                    break;
                }
            }
        }
        else { /* handle error */ }
    }

    unsigned char mac_address[6];

    if (success){
        memcpy(mac_address, ifr.ifr_hwaddr.sa_data, 6);
        printf(" %02x:%02x:%02x:%02x:%02x:%02x\n",mac_address[0],mac_address[1],mac_address[2],mac_address[3],mac_address[4],mac_address[5]);
    }
    memcpy(it,mac_address, 6); it+=6;
    bzero(it,202); it+=202;
    int magic = 0x63538263;
    memcpy(it,&magic,4); it+=4;
    if (type == 1) {
        *it = 53; it++;
        *it = 1; it++;
        *it = 1; it++;
    }
    *it = 0xFF; it+=1;
    return it;
}
//Отправка запроса на сервер
void sendRequestToServer(int socket, char flag){


}

//Получаем ответ от сервера
void getResponseFromServer(){
    uchar* it = msg;
    recv(sock,msg,244,MSG_WAITALL);
    it += 244;
    uchar code = 0;
    while (code != 0xFF) {
        recv(sock,it,1,MSG_WAITALL);
        code = *it; it++;
        switch(code) {
            default: {
                break;
            }
        }
        printf("%d\n",code);
        recv(sock, it, 1, MSG_WAITALL); it++;
        recv(sock, it, *(it-1), MSG_WAITALL); it+=*(it-1);
    }
}

//argv[2] - host; argv[3] - port
int main(int argc, char *argv[]) {
    int sock_fd;
    uint16_t port_no;
    struct sockaddr_in serv_addr{};
    struct hostent *server;

    if( (sock = socket(AF_INET, SOCK_RAW, IPPROTO_UDP)) == -1)
    {
        perror("socket : ");
        return -1;
    }

    int broadcast = 1;
    if( setsockopt(sock, SOL_SOCKET, SO_BROADCAST, &broadcast, sizeof(broadcast)) != 0 )
    {
        perror("setsockopt : ");
        close(sock);
        return -1;
    }
    char netif[] = "enp2s0";
    if (setsockopt(sock, SOL_SOCKET, SO_BINDTODEVICE, netif, sizeof(netif)) != 0)
    {
        perror("setsockopt : ");
        close(sock);
        return -1;
    }
    sockaddr_in s;
    s.sin_addr.s_addr = INADDR_ANY;
    s.sin_port = htons(68);
    bzero(&s.sin_zero, 8);
    s.sin_family = AF_INET;
//    if (bind(sock,(sockaddr*)&s,sizeof(s)) < 0) return 0;
//    u_int16_t src_port, dst_port;
//    u_int32_t src_addr, dst_addr;
//    src_addr = inet_addr(argv[1]);
//    dst_addr = inet_addr(argv[3]);
//    src_port = atoi(argv[2]);
//    dst_port = atoi(argv[4]);

//    int sock;
    uchar* buffer = msg;
    iphdr *ip = (struct iphdr *) buffer;
    udphdr *udp = (struct udphdr *) (buffer + sizeof(iphdr));

    struct sockaddr_in sin;
    int one = 1;
    const int *val = &one;
    int PCKT_LEN = sizeof(iphdr) + sizeof(udphdr) + 576;
    memset(buffer, 0, PCKT_LEN);

    // create a raw socket with UDP protocol
//    sd = socket(PF_INET, SOCK_RAW, IPPROTO_UDP);
    if (sock < 0) {
        perror("socket() error");
        exit(2);
    }
    printf("OK: a raw socket is created.\n");

    // inform the kernel do not fill up the packet structure, we will build our own
//    if(setsockopt(sock, IPPROTO_IP, IP_HDRINCL, val, sizeof(one)) < 0) {
//        perror("setsockopt() error");
//        exit(2);
//    }
//    printf("OK: socket option IP_HDRINCL is set.\n");

//    sin.sin_family = AF_INET;
//    sin.sin_port = htons(dst_port);
//    sin.sin_addr.s_addr = inet_addr("127.0.0.1");
//
//    // fabricate the IP header
//    ip->ihl      = 5;
//    ip->version  = 4;
//    ip->tos      = 16; // low delay
//    ip->tot_len  = sizeof(struct iphdr) + sizeof(struct udphdr);
//    ip->id       = htons(54321);
//    ip->ttl      = 64; // hops
//    ip->protocol = 17; // UDP
//    // source IP address, can use spoofed address here
//    ip->saddr = src_addr;
//    ip->daddr = dst_addr;
//
//    // fabricate the UDP header
//    udp->source = htons(src_port);
//    // destination port number
//    udp->dest = htons(dst_port);
//    udp->len = htons(sizeof(struct udphdr));
//
//    // calculate the checksum for integrity
//    ip->check = csum((unsigned short *)buffer,
//                     sizeof(struct iphdr) + sizeof(struct udphdr));
    uchar* end = formMsg(1);
    char ipd[] = "255.255.255.255";
    struct sockaddr_in si{};
    si.sin_family = AF_INET;
    si.sin_port   = htons( 67 );
    inet_aton( ipd, &si.sin_addr);

    /* send data */
    size_t nBytes = send(sock, msg, end-msg, 0);

    printf("Sent msg: %s, %d bytes with socket %d to %s\n", msg, nBytes, sock, ip);
    getResponseFromServer();

    return 0;
}