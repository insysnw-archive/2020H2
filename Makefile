CC = gcc
CFLAGS = -g -O0 -Wall
headers = server.h dns_protocol.h
objects = server.o main.o dns_protocol.o

.c.o:
	$(CC) $(CFLAGS) -c $<

all: dns_server

dns_server: server.o main.o dns_protocol.o
	$(CC) $(CFLAGS) -o server.out server.o main.o dns_protocol.o

$(objects): $(headers)

clean:
	rm -f $(objects) server.out
