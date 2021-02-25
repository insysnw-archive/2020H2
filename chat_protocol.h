#ifndef __CHAT_PROTOCOL_H__
#define __CHAT_PROTOCOL_H__

#include <inttypes.h>

#define PACKET_SIZE 252
#define HEADER_SIZE 12

struct chat_packet
{
	u_int32_t time;
	u_int32_t nick_size;
	char *nick;
	u_int32_t data_size;
	char *data;
};

void chat_print_packet(struct chat_packet *pkt);
int chat_packet_receive(struct chat_packet *pkt, int sock);
int chat_packet_send(struct chat_packet *pkt, int sock);

#endif