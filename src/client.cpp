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
uchar yip[4];
uchar mask[4];
uchar host[4];
uchar msg[576];
int atime;
int xid = rand();
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
    } else if (type == 2) {
        *it = 53; it++;
        *it = 1; it++;
        *it = 3; it++;
        *it = 50; it++;
        *it = 4; it++;
        memcpy(it,yip,4); it+=4;
        *it = 54; it++;
        *it = 4; it++;
        memcpy(it,host,4); it+=4;
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
    int r = recv(sock,msg,576,MSG_WAITALL);
    memcpy(yip, msg+16, 4);
    it += 240;
    uchar code = 0;
    while (code != 0xFF) {
        code = *it; it++;
        switch(code) {
            case 53: {
                if (*(it+1) == 2) {
                    printf("Offer\n");
                }
                if (*(it+1) == 5) {
                    printf("ACK\n");
                }
                break;
            }
            case 54: {
                memcpy(host,it+1,*it);
                break;
            }
            case 1: {
                memcpy(mask, it+1, *it);
                break;
            }
            case 51: {
                memcpy(&atime, it+1, *it);
                atime = ntohl(atime);
                break;
            }
        }
        it++;
        it+=*(it-1);
    }
}

//argv[2] - host; argv[3] - port
int main(int argc, char *argv[]) {
    if( (sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == -1)
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
    if (bind(sock,(sockaddr*)&s,sizeof(s)) < 0) printf("Failed to bind\n");

    uchar* end = formMsg(1);
    char ipd[] = "255.255.255.255";
    struct sockaddr_in si{};
    si.sin_family = AF_INET;
    si.sin_port   = htons( 67 );
    inet_aton( ipd, &si.sin_addr);

    /* send data */
    size_t nBytes = sendto(sock, msg, end-msg, 0,
                           (sockaddr*)&si,(socklen_t)sizeof(si));

    printf("Sent msg: %s, %d bytes with socket %d to %s\n", msg, nBytes, sock, ipd);
    getResponseFromServer();
    printf("New ip: %d.%d.%d.%d\n",yip[0],yip[1],yip[2],yip[3]);
    printf("DHCP server: %d.%d.%d.%d\n",host[0],host[1],host[2],host[3]);
    printf("Mask: %d.%d.%d.%d\n",mask[0],mask[1],mask[2],mask[3]);
    end = formMsg(2);
    nBytes = sendto(sock, msg, end-msg, 0,
                           (sockaddr*)&si,(socklen_t)sizeof(si));
    getResponseFromServer();
    printf("New ip: %d.%d.%d.%d\n",yip[0],yip[1],yip[2],yip[3]);
    printf("DHCP server: %d.%d.%d.%d\n",host[0],host[1],host[2],host[3]);
    printf("Mask: %d.%d.%d.%d\n",mask[0],mask[1],mask[2],mask[3]);
    printf("Lease time: %d hours\n", atime/3600);
    return 0;
}