CFLAGS := -g -Wall
CC := cc

objects = client.o server.o chat_protocol.o server_blocking.o
headers = chat_protocol.h

.c.o:
	$(CC) $(CFLAGS) -c $<

all: server client server_blocking

client: client.o chat_protocol.o
	$(CC) $(CFLAGS) client.o chat_protocol.o -o client.out

server: server.o chat_protocol.o
	$(CC) $(CFLAGS) server.o chat_protocol.o -o server.out
	
server_blocking: server_blocking.o chat_protocol.o
	$(CC) $(CFLAGS) server_blocking.o chat_protocol.o -o server_blocking.out -pthread

$(objects): $(headers)

clean:
	rm -f $(objects) client server server_blocking client.out server.out server_blocking.out