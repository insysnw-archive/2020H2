import select
import socket
import sys

host, port = sys.argv[1], int(sys.argv[2])
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind((host, port))
server.listen(100)

clients = [server]
users = {}

print("Server ready to work")


def broadcast(sock, nick, msg, header):
    for tmp in clients:
        if tmp != sock and tmp != server:
            tmp.send(nick['header'] + nick['data'] + header + msg)


def get_message(client):
    try:
        header = client.recv(5)
    except ConnectionResetError or OSError:
        return False
    if not len(header):
        return False

    size = int.from_bytes(header, byteorder='big', signed=False)
    data = client.recv(size)
    if data is None:
        return False
    return {'header': header, 'data': data}


# def handle_connections(client):
#     if client not in clients:
#         user = get_message(client)
#         if user is False:
#             return
#
#         print(users, user)
#         if user in users.values():
#             msg = "Username already exists"
#             client.send(len("SERVER").to_bytes(5, byteorder='big') + "SERVER".encode() + len(msg).to_bytes(5, byteorder='big') + msg.encode())
#             client.close()
#             return
#         users[client] = user
#         clients.add(client)
#
#         print("User {} is joined to the server".format(user['data'].decode('ascii')))
#
#         msg = f"{user['data'].decode('ascii')} joined to Server".encode('ascii')
#         msg_header = len(msg).to_bytes(5, byteorder='big')
#
#         broadcast(client, user, msg, msg_header)
#
#     while True:
#         msg = get_message(client)
#         user = users[client]
#
#         if msg is False:
#             print(' {} left the server'.format(users[client]['data'].decode('ascii')))
#             msg = f"{user['data'].decode('ascii')} disconnected from Server".encode('ascii')
#             msg_header = len(msg).to_bytes(5, byteorder='big')
#             broadcast(client, user, msg, msg_header)
#             clients.remove(client_socket)
#             del users[client_socket]
#             break
#
#         broadcast(client, user, msg['data'], msg['header'])


while True:
    read_sockets, _, exception_sockets = select.select(clients, [], clients)

    for some_socket in read_sockets:
        if some_socket == server:
            client_socket, address_socket = server.accept()
            user = get_message(client_socket)

            if user is False:
                continue
            if user in users.values():
                msg = "Username already exists"
                client_socket.send(len("SERVER").to_bytes(5, byteorder='big') + "SERVER".encode() + len(msg).to_bytes(5, byteorder='big') + msg.encode())
                client_socket.close()
                continue

            clients.append(client_socket)
            users[client_socket] = user
            print("User {} is joined to the server".format(user['data'].decode('ascii')))

            msg = f"{user['data'].decode('ascii')} joined to Server".encode('ascii')
            msg_header = len(msg).to_bytes(5, byteorder='big')

            broadcast(client_socket, user, msg, msg_header)

        else:
            msg = get_message(some_socket)
            if msg is False:
                user = users[some_socket]
                print(' {} left the server'.format(users[some_socket]['data'].decode('ascii')))
                msg = f"{user['data'].decode('ascii')} disconnected from Server".encode('ascii')
                msg_header = len(msg).to_bytes(5, byteorder='big')
                broadcast(some_socket, user, msg, msg_header)
                clients.remove(some_socket)
                del users[some_socket]
                continue

            user = users[some_socket]
            broadcast(some_socket, user, msg['data'], msg['header'])
