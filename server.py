import socket
import threading
import time
import sys

# check args
if len(sys.argv) != 3:
    print("Select IP and port to create a server")
    sys.exit()
host = sys.argv[1]
port = int(sys.argv[2])

# server starting
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind((host, port))
server.listen()
print(f"Server online")

# nickname/clients list
clients = []
nicknames = {}


# broadcast messages send with no self-messaging
def broadcast(sock, nick, msg, header):
    for client_sock in clients:
        if client_sock != sock:
            client_sock.send(nick['header'] + nick['data'] + header + msg)


# receiver for clients
def receive(client):
    while True:
        try:
            # get header then message
            header = client.recv(5)
        except ConnectionResetError:
            return False
        if not len(header):
            return False
        length = int.from_bytes(header, byteorder='big', signed=False)
        return {'header': header, 'data': client.recv(length)}


# handler method for clients
def handle(client, address):
    if client not in clients:
        # add new nick
        nickname = receive(client)
        if nickname is False:
            return
        nicknames[client] = nickname
        clients.append(client)
        print("<{}> Connected with {}".format(time.strftime('%H:%M', time.localtime()), str(address)))
        print("Nickname is {}".format(nickname['data'].decode('ascii')))
        message = f"{nickname['data'].decode('ascii')} joined!".encode('ascii')
        message_header = len(message).to_bytes(5, byteorder='big')
        broadcast(client, nickname, message, message_header)
    while True:
        # broadcast hearing
        message = receive(client)
        nickname = nicknames[client]
        if message is False:
            print('Connection from {} was closed'.format(nicknames[client]['data'].decode('ascii')))
            message = f"User {nickname['data'].decode('ascii')} left the chat".encode('ascii')
            message_header = len(message).to_bytes(5, byteorder='big')
            broadcast(client, nickname, message, message_header)
            clients.remove(_client)
            del nicknames[_client]
            break
        broadcast(client, nickname, message['data'], message['header'])


# let's go
while True:
    _client, _address = server.accept()
    thread = threading.Thread(target=handle, args=(_client, _address))
    thread.start()
