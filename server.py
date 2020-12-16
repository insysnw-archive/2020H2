import socket
import threading
import time

# connection
host = '127.0.0.1'
port = 9097

# start
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind((host, port))
server.listen()

clients = []
nicknames = {}

print(f"Server Started!")


# send to all clients except the sender
def broadcast(sock, nick, msg, header):
    for tmp in clients:
        if tmp != sock:
            tmp.send(nick['header'] + nick['data'] + header + msg)


# receive messages from clients
def receive(client):
    while True:
        try:
            header = client.recv(5)
        except ConnectionResetError:
            return False

        if not len(header):
            return False

        length = int.from_bytes(header, byteorder='big', signed=False)

        return {'header': header, 'data': client.recv(length)}


def handle(client, address):
    if client not in clients:
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
        message = receive(client)
        nickname = nicknames[client]

        if message is False:
            print('Connection from {} was closed'.format(nicknames[client]['data'].decode('ascii')))
            message = f"User {nickname['data'].decode('ascii')} left the chat".encode('ascii')
            message_header = len(message).to_bytes(5, byteorder='big')
            broadcast(client, nickname, message, message_header)
            clients.remove(client_socket)
            del nicknames[client_socket]
            break

        broadcast(client, nickname, message['data'], message['header'])


while True:
    # thread for clients
    client_socket, address_socket = server.accept()
    thread = threading.Thread(target=handle, args=(client_socket, address_socket)).start()
