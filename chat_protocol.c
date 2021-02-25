#include <arpa/inet.h>

#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <errno.h>
#include <string.h>
#include <time.h>

#include "chat_protocol.h"

void chat_print_packet(struct chat_packet *pkt)
{
	time_t rawtime = (time_t)pkt->time;

	printf("TIME: %s", ctime(&rawtime));
	printf("nick_size: %d\n", pkt->nick_size);
	printf("nick: %s\n", pkt->nick);
	printf("data_size: %d\n", pkt->data_size);
	printf("data: %s\n\n", pkt->data);
}

int chat_packet_receive(struct chat_packet *pkt, int sock)
{
	int res;
	//Чтение времени и длины ника
	res = recv(sock, &pkt->time, 4, MSG_WAITALL);
	if (res <= 0)
		return res;
	res = recv(sock, &pkt->nick_size, 4, MSG_WAITALL);
	if (res < 0)
		return res;
	//Чтение ника
	pkt->nick = calloc(pkt->nick_size, sizeof(char));
	res = recv(sock, pkt->nick, pkt->nick_size, MSG_WAITALL);
	if (res < 0)
		return res;
	//Чтение длины сообщения и его самого
	res = recv(sock, &pkt->data_size, 4, MSG_WAITALL);
	if (res < 0)
		return res;
	pkt->data = calloc(pkt->data_size, sizeof(char));
	res = recv(sock, pkt->data, pkt->data_size, MSG_WAITALL);
	if (res < 0)
		return res;
	return 1;
}

int chat_packet_send(struct chat_packet *pkt, int sock)
{
	//Посылаем время и длину ника
	int res = send(sock, &pkt->time, 4, 0);
	if (res <= 0)
		return res;
	res = send(sock, &pkt->nick_size, 4, 0);
	if (res <= 0)
		return res;
	//Посылаем ник
	res = send(sock, pkt->nick, pkt->nick_size, 0);
	if (res <= 0)
		return res;
	//Посылаем длину данных и данные
	res = send(sock, &pkt->data_size, 4, 0);
	if (res <= 0)
		return res;
	res = send(sock, pkt->data, pkt->data_size, 0);
	if (res <= 0)
		return res;

	return 1;
}