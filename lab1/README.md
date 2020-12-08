
## Description

The server gets messages from the client and sends them back to all clients.

### Launch
Server:
>python3 server.py 

Client:
>python3 client.py [Server IP] [Server port]

### Server
1. Create a socket
2. Bind to address
3. Listen and wait for connections
4. Accept connections
5. Send/recv data

### Client
1. Create a socket
2. Connect to the server
3. Wait for nickname
4. Send/recv data

### Packet format
From client:
1. 1 byte - 0 or 1
0 - packet with message
1 - packet from new user with nick
2. 5 bytes - length of nick/message
3. Previous num of length bytes - nick/message

From server:
1. 1 byte - 0 or 1 
if 1: packet is the same as from client
else:
2. 1 byte - time in hours
3. 1 byte - time in minutes
4. 5 bytes - length of message
5. Previous num of length bytes - message

Message in terminal:
<HH:MM> [username] Text of an actual message
