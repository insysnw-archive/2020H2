CC = gcc
CFLAGS = -g -O0 -Wall
headers = snmp_protocol.h
objects = client.o snmp_protocol.o

.c.o:
	$(CC) $(CFLAGS) -c $<

all: snmp_client

snmp_client: client.o snmp_protocol.o
	$(CC) $(CFLAGS) -o client.out client.o snmp_protocol.o -pthread

$(objects): $(headers)

clean:
	rm -f $(objects) client.out
