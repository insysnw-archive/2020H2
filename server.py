import socket
import threading
import time

# connection
host = '127.0.0.1'
port = 55555

# start
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind((host, port))
server.listen()

clients = []
nicknames = []

print("Server Started")


# send to all clients
def broadcast(message):
    for client in clients:
        client.send(message)


# handle messages from clients
def handle(client):
    while True:
        try:
            message = client.recv(1024)
            broadcast(message)

        except:
            index = clients.index(client)
            clients.remove(client)
            nickname = nicknames[index]
            broadcast("<{}> {} left".format(time.strftime('%H:%M', time.localtime()), nickname).encode('ascii'))
            print("<{}> {} left".format(time.strftime('%H:%M', time.localtime()), nickname))
            break


def receive():
    while True:
        client, address = server.accept()
        print("<{}> Connected with {}".format(time.strftime('%H:%M', time.localtime()), str(address)))

        # store clients and nicknames
        client.send('NICK'.encode('ascii'))
        nickname = client.recv(1024).decode('ascii')
        nicknames.append(nickname)
        clients.append(client)

        # print and broadcast nicknames
        print("Nickname is {}".format(nickname))
        broadcast("<{}> {} joined".format(time.strftime('%H:%M', time.localtime()), nickname).encode('ascii'))

        # thread for clients
        thread = threading.Thread(target=handle, args=(client,))
        thread.start()


receive()
