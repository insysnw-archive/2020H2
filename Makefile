CC = gcc
CFLAGS = -g -O0 -Wall
headers = protocol.h
objects = server.o protocol.o

.c.o:
	$(CC) $(CFLAGS) -c $<

all: server

server: server.o protocol.o
	$(CC) $(CFLAGS) -o server.out server.o protocol.o -pthread

$(objects): $(headers)

clean:
	rm -f $(objects) server.out
