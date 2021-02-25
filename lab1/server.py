import socket
import sys
import threading

if len(sys.argv) != 3:
    print("Specify IP and port")
    sys.exit()

address = (sys.argv[1], int(sys.argv[2]))
clients = list()
users = dict()

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(address)
server.listen()
print("Server ready to work")


def getMessage(client):
    while True:
        try:
            header = client.recv(5)
        except ConnectionError:
            return False
        if not len(header):
            return False

        size = int.from_bytes(header, byteorder='big', signed=False)
        return {'header': header, 'data': client.recv(size)}


def handleConnections(client):
    if client not in clients:
        user = getMessage(client)
        if user is False:
            return
        if user['data'] in users.values():
            msg = "Such login is already in use "
            msgHeader = len(msg).to_bytes(5, byteorder='big')
            client.send(user['header'] + user['data'] + msgHeader + msg)
        else:
            users[client] = user

            clients.append(client)
            msg = f"{user['data'].decode('')} joined to Server".encode()
            msgHeader = len(msg).to_bytes(5, byteorder='big')

            for anyClinet in clients:
                anyClinet.send(user['header'] + user['data'] + msgHeader + msg)

    while True:
        message = getMessage(client)
        user = users[client]
        if message is False:
            msg = f"{user['data'].decode()} disconnected from Server".encode()
            msgHeader = len(msg).to_bytes(5, byteorder='big')
            for anyClinet in clients:
                if anyClinet != client:
                    anyClinet.send(user['header'] + user['data'] + msgHeader + msg)
            clients.remove(connSocket)
            break
        for anyClinet in clients:
            if anyClinet != client:
                anyClinet.send(user['header'] + user['data'] + message['header'] + message['data'])


if __name__ == '__main__':
    while True:
        connSocket, addrSocket = server.accept()
        thread = threading.Thread(target=handleConnections, args=(connSocket, addrSocket))
        thread.start()
