#ifndef __PROTOCOL_H__
#define __PROTOCOL_H__

#include <sys/types.h>

#define REGISTER_REQUEST 0
#define REGISTER_RESPONSE 1
#define TEST_NUMBER 2
#define QUESTION 3
#define QUESTION_RESPONSE 4
#define STRING_ARG 5
#define REQUEST_LAST_RESULT 6
#define REQUEST_ALL_TEST 7

struct protocol_packet
{
	u_int8_t type;
	u_int16_t error_code;
	u_int16_t login_size;
	u_int16_t password_size;
	u_int16_t question_size;
	u_int16_t answer_size;
	u_int16_t str_arg_size;
	u_int16_t test_number;
	char *login;
	char *password;
	char *question;
	char *answer;
	char *str_arg;
};

void packet_print(struct protocol_packet *pkt);

int packet_parse(struct protocol_packet *pkt, void *data, u_int16_t size);
int packet_pack(struct protocol_packet *pkt, void *packed);

#endif