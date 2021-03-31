#include <arpa/inet.h>

#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <errno.h>
#include <string.h>

#include "protocol.h"

void packet_print(struct protocol_packet *pkt)
{
    printf("------------------------------------------------------\n");
    printf("Request or response: %d\n", pkt->qr);
    if (pkt->qr == CODE_REQUEST)
    {
        printf("Command code: %d\n", pkt->request.command);
        printf("Arg size: %d\n", pkt->request.arg_size);
        printf("Args: %s\n", pkt->request.args);
    }
    else
    {
        printf("Error code: %d\n", pkt->response.error_code);
        printf("Arg size: %d\n", pkt->response.arg_size);
        printf("Args: %s\n", pkt->response.args);
    }
    printf("------------------------------------------------------\n");
}

int pack_arg(char *packed, void *to_pack, int type)
{
    *packed = type;
    int len = (u_int8_t)strlen(((char *)to_pack));
    memcpy(packed + 1, &len, 1);

    memcpy(packed + 2, to_pack, strlen(((char *)to_pack)));
    return strlen(((char *)to_pack)) + 2;
}

int parse_arg(char *parsed, char *to_parse)
{
    u_int8_t type = (u_int8_t) * (to_parse);
    u_int8_t size = (u_int8_t) * (to_parse + 1);
    //parsed = calloc(size, sizeof(char));

    memcpy(parsed, to_parse + 2, size);
    return size + 2;
}

int packet_parse(struct protocol_packet *pkt, void *data, u_int16_t size)
{
    memcpy(&pkt->qr, data, 1);
    if (pkt->qr == CODE_REQUEST)
    {
        //pkt->request = calloc(1, sizeof(struct protocol_request));
        memcpy(&pkt->request.command, data + 1, 1);
        memcpy(&pkt->request.arg_size, data + 2, 2);
        pkt->request.arg_size = ntohs(pkt->request.arg_size);
        pkt->request.args = calloc(size - 4, sizeof(char));
        memcpy(pkt->request.args, data + 4, size - 4);
    }
    else
    {
        //pkt->response = calloc(1, sizeof(struct protocol_response));
        if (pkt->qr == CODE_RESPONSE)
        {
            memcpy(&pkt->response.error_code, data + 1, 1);
            memcpy(&pkt->response.arg_size, data + 2, 2);
            pkt->response.arg_size = ntohs(pkt->response.arg_size);
            pkt->response.args = calloc(size - 4, sizeof(char));
            memcpy(pkt->response.args, data + 4, size - 4);
        }
        else
        {
            return -1;
        }
    }

    return 1;
}

int request_pack(struct protocol_packet *pkt, void *packed)
{
    int size = 0;
    memcpy(packed, pkt, 1);
    pkt->request.arg_size = htons(pkt->request.arg_size);
    memcpy(packed + 1, &pkt->request.command, 1);
    memcpy(packed + 2, &pkt->request.arg_size, 2);
    size += 4;

    memcpy(packed + size, pkt->request.args, ntohs(pkt->request.arg_size));
    size += ntohs(pkt->request.arg_size);
    return size;
}

int response_pack(struct protocol_packet *pkt, void *packed)
{
    int size = 0;
    memcpy(packed, pkt, 1);
    pkt->response.arg_size = htons(pkt->response.arg_size);
    memcpy(packed + 1, &pkt->response.error_code, 1);
    memcpy(packed + 2, &pkt->response.arg_size, 2);
    size += 4;

    memcpy(packed + size, pkt->response.args, ntohs(pkt->response.arg_size));
    size += ntohs(pkt->response.arg_size);
    return size;
}