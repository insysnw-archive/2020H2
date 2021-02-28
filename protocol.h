#ifndef __PROTOCOL_H__
#define __PROTOCOL_H__

#include <sys/types.h>

#define CODE_REQUEST 0
#define CODE_RESPONSE 1

#define COMMAND_LOGIN 0
#define COMMAND_LS 1
#define COMMAND_CD 2
#define COMMAND_WHO 3
#define COMMAND_KILL 4
#define COMMAND_LOGOUT 5

#define COMMAND_ARG 1
#define LOGIN_ARG 2
#define PASS_ARG 3
#define RESPONSE_STRING 4

struct protocol_request
{
    u_int8_t command;
    u_int16_t arg_size;
    char *args;
};

struct protocol_response
{
    u_int8_t error_code; //0 if success
    u_int16_t arg_size;
    char *args;
};

struct protocol_packet
{
    u_int8_t qr;
    struct protocol_request request;
    struct protocol_response response;
};

void packet_print(struct protocol_packet *pkt);

int pack_arg(char *packed, void *to_pack, int type);
int parse_arg(char *parsed, char *to_parce);

int packet_parse(struct protocol_packet *pkt, void *data, u_int16_t size);
int request_pack(struct protocol_packet *pkt, void *packed);
int response_pack(struct protocol_packet *pkt, void *packed);


#endif