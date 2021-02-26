import socket
import sys
import threading

host, port = sys.argv[1], int(sys.argv[2])
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind((host, port))
server.listen(100)

clients = set()
users = {}

print("Server ready to work")


def broadcast(sock, nick, msg, header):
    for tmp in clients:
        if tmp != sock:
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


def handle_connections(client):
    if client not in clients:
        user = get_message(client)
        if user is False:
            return

        print(users, user)
        if user in users.values():
            msg = "Username already exists"
            client.send( len("SERVER").to_bytes(5, byteorder='big') + "SERVER".encode() + len(msg).to_bytes(5, byteorder='big') + msg.encode())
            client.close()
            return
        print('WTF')
        users[client] = user
        clients.add(client)

        print("User {} is joined to the server".format(user['data'].decode('ascii')))

        msg = f"{user['data'].decode('ascii')} joined to Server".encode('ascii')
        msg_header = len(msg).to_bytes(5, byteorder='big')

        broadcast(client, user, msg, msg_header)

    while True:
        msg = get_message(client)
        user = users[client]

        if msg is False:
            print(' {} left the server'.format(users[client]['data'].decode('ascii')))
            msg = f"{user['data'].decode('ascii')} disconnected from Server".encode('ascii')
            msg_header = len(msg).to_bytes(5, byteorder='big')
            broadcast(client, user, msg, msg_header)
            clients.remove(client_socket)
            del users[client_socket]
            break

        broadcast(client, user, msg['data'], msg['header'])


while True:
    client_socket, address_socket = server.accept()
    print(f'Connected: {address_socket}')
    thread = threading.Thread(target=handle_connections, args=(client_socket,), daemon=True).start()
