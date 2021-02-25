import socket
import threading
from datetime import datetime

# connection
host = '0.0.0.0'
port = 2020

# start
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind((host, port))
server.listen(20)

names = {}
print(f"Server Started!")


def read_block(client):
    data_len = int.from_bytes(client.recv(2), byteorder='big')
    data = client.recv(data_len).decode('utf-8')
    return data


def read_bytes(client):
    data_len = client.recv(2)
    length = int.from_bytes(data_len, byteorder='big')
    data = client.recv(length)
    return data_len + data


def block_of(data): //блок данных (сама строка)
    data_enc = data.encode('utf-8')
    return len(data_enc).to_bytes(2, byteorder='big') + data_enc


def client_work(client):
    my_name = ""
    while True:
        try:
            flag = int.from_bytes(client.recv(1), byteorder='big')
            if flag == 1:
                name = read_block(client)
                try:
                    exist = names[name]
                    client.send(b'\xC1')
                    print("{} already authorized as {}".format(name, exist.getpeername()))
                except KeyError:
                    my_name = name
                    names[name] = client
                    client.send(b'\x81')
            elif flag == 2:
                name = read_block(client)
                try:
                    if my_name == "":
                        client.send(b'\xC2')
                    else:
                        enc_time = int(datetime.utcnow().timestamp()).to_bytes(4, byteorder='big')
                        names[name].send(b'\x82' + block_of(my_name) + read_bytes(client) + enc_time)
                        client.send(b'\x81')
                except KeyError:
                    client.send(b'\xC2')
            elif flag == 3:
                if my_name == "":
                    client.send(b'\xC3')
                else:
                    enc_time = int(datetime.utcnow().timestamp()).to_bytes(4, byteorder='big')
                    msg = read_bytes(client)
                    for c in names.values():
                        c.send(b'\x82' + block_of(my_name) + msg + enc_time)
                    client.send(b'\x81')

        except ConnectionResetError:
            try:
                names.pop(my_name)
                print("{} disconnected".format(my_name))
            except KeyError:
                print("Disconnected unauthorized client")
            print('Connection from {} was closed'.format(client.getpeername()))
            break


while True:
    # thread for clients
    client_socket, address = server.accept()
    thread = threading.Thread(target=client_work, args=[client_socket]).start()
