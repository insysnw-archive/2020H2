CC = gcc
CFLAGS = -g -O0 -Wall
headers = protocol.h
objects = client.o protocol.o

.c.o:
	$(CC) $(CFLAGS) -c $<

all: client

client: client.o protocol.o
	$(CC) $(CFLAGS) -o client.out client.o protocol.o

$(objects): $(headers)

clean:
	rm -f $(objects) client.out
