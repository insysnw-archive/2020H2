# TCP

The server gets messages from the client and sends them back to other clients (broadcast messaging).

``` bash
$python3 client.py
$python3 server.py
or
TCP_Chat/.vscode/launch.json
```
---

## Description

**Server Socket**

1. **Create** a socket.
2. **Bind** to an address.
3. **Listen** on a port and wait for a connection to be established.
4. **Accept** the connection from a client.
5. **Send/Recv** - the same way we read and write for a file.
---

**Client Socket**

1. **Create** a socket.
2. **Connect** to a server.
3. **Send/Recv** - repeat until we have or receive data.

<!-- ![Markdown Logo](https://media.geeksforgeeks.org/wp-content/uploads/20190715192804/ClientServerSocket.jpg) -->

![Markdown Logo](https://www.codeproject.com/KB/IP/1264257/sdgfh.jpg)
---

## Message format

Message format that the server and clients use to communicate.
```
 square_bracket      time       square_bracket      author      greater_than      message
|--- 1 byte ---|--- 5 bytes ---|--- 1 byte ---|--- n bytes ---|--- 1 byte ---|--- m bytes ---|

Example: [12:00] user_name > message
```
---

Python 3 provides the setblocking () method, which passes a parameter equal to 0. The difference between lab 1b and lab 1a is that 1b uses non-blocking sockets, while lab 1a uses a separate thread for each client to process multiple clients on the same server.

---

## Arguments

### Both for server and client:
``` python
TIME_LENGTH = 5
HEADER_LENGTH = 30
ENCODING = 'utf-8'

HOSTNAME = socket.gethostname()
PORT = 1024
```

### Just for server:
```python
MAX_CONNECTIONS = 50
EXIT = 0
PID = os.getpid()
```