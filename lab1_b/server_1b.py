import socket
import select
import sys

if len(sys.argv) != 3:
    print("Select IP and port!")
    sys.exit()

ip = sys.argv[1]
port = int(sys.argv[2])

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind((ip, port))
server.listen()

sockets_list = [server]  # list of sockets
clients = {}  # list of connected clients

print(f'Server is started!')


def broadcast(sock, nick, msg, header):
    for tmp in clients:
        if tmp != sock:
            tmp.send(nick['header'] + nick['data'] + header + msg)


def receive_message(sock):
    try:
        header = sock.recv(5)
    except ConnectionResetError:
        return False

    if not len(header):
        return False

    length = int.from_bytes(header, byteorder='big', signed=False)

    return {'header': header, 'data': sock.recv(length)}


def close_connection(sock):
    nick = clients[sock]
    print(f"Connection from {clients[sock]['data'].decode('ascii')} was closed")
    data = f"{nick['data'].decode('ascii')} left the chat".encode('ascii')
    header = len(data).to_bytes(5, byteorder='big')

    broadcast(some_socket, nick, data, header)

    sockets_list.remove(sock)
    del clients[sock]


while True:
    read_sockets, _, exception_sockets = select.select(sockets_list, [], sockets_list)

    for some_socket in read_sockets:
        if some_socket == server:
            client_socket, client_address = server.accept()
            nickname = receive_message(client_socket)

            if nickname is False:
                continue

            if nickname in clients.values():
                ms = f"Nickname {nickname['data'].decode('ascii')} is already in use!".encode('ascii')
                header = len(ms).to_bytes(5, byteorder='big')
                client_socket.send(nickname['header'] + nickname['data'] + header + ms)
                continue

            sockets_list.append(client_socket)
            clients[client_socket] = nickname

            print(f"{nickname['data'].decode('ascii')} was connected from {client_address}")

            message = f"{nickname['data'].decode('ascii')} joined!".encode('ascii')
            message_header = len(message).to_bytes(5, byteorder='big')

            broadcast(client_socket, nickname, message, message_header)

        else:
            message = receive_message(some_socket)

            if message is False:
                close_connection(some_socket)
                continue

            nickname = clients[some_socket]
            broadcast(some_socket, nickname, message['data'], message['header'])