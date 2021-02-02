import socket
import threading
import time
import datetime

# connection
host = '127.0.0.1'
port = 9090

# start
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind((host, port))
server.listen(10)

clients = []
nicknames = {}

print(f"Server Started!")


# send to all clients except the sender
def broadcast(sock, nick, t, msg_header, msg):
    for tmp in clients:
        tmp.send(t + nick['header'] + nick['data'] + msg_header + msg)


# receive messages from clients
def receive(client):
    while True:
        try:
            header = client.recv(5)
        except ConnectionResetError:
            print("Connection reset")
            return dict()

        if not len(header):
            print("incorrect length")
            return dict()

        length = int.from_bytes(header, byteorder='big', signed=False)

        return {'header': header, 'data': client.recv(length)}


# def user_exists(client, nickname):
#    print('Connection from {} was refused, because the chosen nickname already exists'.format(nicknames[client]['data'].decode('ascii')))
#    message = f"User {nickname['data'].decode('ascii')} already exists".encode('ascii')
#    message_header = len(message).to_bytes(5, byteorder='big')
#    broadcast(client, nickname, message, message_header)
# clients.remove(client_socket)

def handle(client, address):
    if client not in clients:
        nickname = receive(client)
        print(nickname['data'])
        if nickname is False:
            return

        # for tmp in clients:
        #    if nicknames[tmp] == nickname:
        #        user_exists(tmp, nickname)
        #        return
        #    else:
        #        continue

        nicknames[client] = nickname

        clients.append(client)

        print("<{}> Connected with {}".format(time.strftime('%H:%M', time.localtime()), str(address)))
        print("Nickname is {}".format(nickname['data'].decode('ascii')))
        enc_time = int(datetime.datetime.utcnow().timestamp()).to_bytes(4, byteorder='big')
        message = f"{nickname['data'].decode('ascii')} joined!".encode('ascii')
        message_header = len(message).to_bytes(5, byteorder='big')
        broadcast(client, nickname, enc_time, message_header, message)

    while True:
        nickname = receive(client)
        message = receive(client)
        nickname = nickname
        enc_time = int(datetime.datetime.utcnow().timestamp()).to_bytes(4, byteorder='big')
        if len(message) == 0:
            nickname = nicknames[client]
            print('Connection from {} was closed'.format(nicknames[client]['data'].decode('ascii')))
            enc_time = int(datetime.datetime.utcnow().timestamp()).to_bytes(4, byteorder='big')
            message = f"User {nickname['data'].decode('ascii')} left the chat".encode('ascii')
            message_header = len(message).to_bytes(5, byteorder='big')
            clients.remove(client_socket)
            del nicknames[client_socket]
            broadcast(client, nickname, enc_time, message_header, message)
            break
        print('<{}> [{}] {}'
              .format(datetime.datetime.utcnow().strftime('%H:%M:%S'), nickname['data'], message['data']))
        broadcast(client, nickname, enc_time, message['header'], message['data'])


while True:
    # thread for clients
    client_socket, address_socket = server.accept()
    thread = threading.Thread(target=handle, args=(client_socket, address_socket)).start()
