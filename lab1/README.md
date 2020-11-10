## Description

The server gets messages from the client and sends them back to all clients.

### Launch
Server:
>python server.py 

Client:
>python client.py

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

### Message format
<HH:MM> [username] Text of an actual message


