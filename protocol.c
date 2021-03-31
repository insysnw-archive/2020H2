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
	printf("Packet type: %d\n Error code: %d\n", pkt->type, pkt->error_code);
	printf("test_number: %d\n", pkt->test_number);
	if (pkt->login_size > 0)
	{
		printf("Login(size %d) :%s\n", pkt->login_size, pkt->login);
	}
	if (pkt->password_size > 0)
	{
		printf("Password(size %d) :%s\n", pkt->password_size, pkt->password);
	}
	if (pkt->question_size > 0)
	{
		printf("Question(size %d) :%s\n", pkt->question_size, pkt->question);
	}
	if (pkt->answer_size > 0)
	{
		printf("Answer(size %d) :%s\n", pkt->answer_size, pkt->answer);
	}
	if (pkt->str_arg_size > 0)
	{
		printf("Str_arg(size %d) :%s\n", pkt->str_arg_size, pkt->str_arg);
	}
	printf("------------------------------------------------------\n");
}

int packet_parse(struct protocol_packet *pkt, void *data, u_int16_t size)
{
	//Copy type and error code
	memcpy(pkt, data, 3);
	pkt->login_size = 0;
	pkt->password_size = 0;
	pkt->question_size = 0;
	pkt->answer_size = 0;
	pkt->str_arg_size = 0;
	//Copy arguments depending on what type packet is
	if (pkt->type == REGISTER_REQUEST)
	{
		//Login
		memcpy(&pkt->login_size, data + 3, 2);
		pkt->login = calloc(pkt->login_size, sizeof(char));
		memcpy(pkt->login, data + 5, pkt->login_size);

		//Passsword
		memcpy(&pkt->password_size, data + 5 + pkt->login_size, 2);
		pkt->password = calloc(pkt->password_size, sizeof(char));
		memcpy(pkt->password, data + 7 + pkt->login_size, pkt->password_size);
	}

	if (pkt->type == QUESTION)
	{
		//Question
		memcpy(&pkt->question_size, data + 3, 2);
		pkt->question = calloc(pkt->question_size, sizeof(char));
		memcpy(pkt->question, data + 5, pkt->question_size);
	}

	if (pkt->type == QUESTION_RESPONSE)
	{
		//Question
		memcpy(&pkt->question_size, data + 3, 2);
		pkt->question = calloc(pkt->question_size, sizeof(char));
		memcpy(pkt->question, data + 5, pkt->question_size);

		//Answer
		memcpy(&pkt->answer_size, data + 5 + pkt->question_size, 2);
		pkt->answer = calloc(pkt->answer_size, sizeof(char));
		memcpy(pkt->answer, data + 7 + pkt->question_size, pkt->answer_size);
	}

	if (pkt->type == STRING_ARG)
	{
		memcpy(&pkt->str_arg_size, data + 3, 2);
		pkt->str_arg = calloc(pkt->str_arg_size, sizeof(char));
		memcpy(pkt->str_arg, data + 5, pkt->str_arg_size);
	}

	if (pkt->type == TEST_NUMBER)
	{
		memcpy(&pkt->test_number, data + 3, 2);
	}

	return 1;
}

int packet_pack(struct protocol_packet *pkt, void *packed)
{
	int size = 0;
	//Copy type and error code
	memcpy(packed, pkt, 3);
	size += 3;
	//Copy arguments depending on what type packet is
	if (pkt->type == REGISTER_REQUEST)
	{
		//Login
		memcpy(packed + size, &pkt->login_size, 2);
		memcpy(packed + size + 2, pkt->login, pkt->login_size);
		size = size + 2 + pkt->login_size;

		//Passsword
		memcpy(packed + size, &pkt->password_size, 2);
		memcpy(packed + size + 2, pkt->password, pkt->password_size);
		size = size + 2 + pkt->password_size;
	}

	if (pkt->type == QUESTION)
	{
		//Question
		memcpy(packed + size, &pkt->question_size, 2);
		memcpy(packed + size + 2, pkt->question, pkt->question_size);
		size = size + 2 + pkt->question_size;
	}

	if (pkt->type == QUESTION_RESPONSE)
	{
		//Question
		memcpy(packed + size, &pkt->question_size, 2);
		memcpy(packed + size + 2, pkt->question, pkt->question_size);
		size = size + 2 + pkt->question_size;

		//Answer
		memcpy(packed + size, &pkt->answer_size, 2);
		memcpy(packed + size + 2, pkt->answer, pkt->answer_size);
		size = size + 2 + pkt->answer_size;
	}

	if (pkt->type == STRING_ARG)
	{
		memcpy(packed + size, &pkt->str_arg_size, 2);
		memcpy(packed + size + 2, pkt->str_arg, pkt->str_arg_size);
		size = size + 2 + pkt->str_arg_size;
	}

	if (pkt->type == TEST_NUMBER)
	{
		memcpy(packed + size, &pkt->test_number, 2);
		size += 2;
	}

	return size;
}