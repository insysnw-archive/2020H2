import socket
import threading
import os
import time
import datetime
from os import path

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
prod = {}
nextId = 1
print(f"Server Started!")


def handle(client):
    cur = path.abspath(os.getcwd())
    global nextId
    while True:
        try:
            flag = int.from_bytes(client.recv(1), byteorder='big')
            if flag == 1:
                print("Получение файла")
                name_len = int.from_bytes(client.recv(2), byteorder='big')
                name = client.recv(name_len)
                file_len = int.from_bytes(client.recv(4), byteorder='big')
                file = client.recv(file_len)
                try:
                    f = open(path.join(cur, name.decode('utf-8')), 'wb')
                    f.write(file)
                    f.close()
                    client.send(0x81.to_bytes(1, byteorder='big'))
                except:
                    client.send(0xC1.to_bytes(1, byteorder='big'))
            elif flag == 2:
                print("Просмотр каталога")
                client.send(0x83.to_bytes(1, byteorder='big'))
                cnt = len(os.listdir(cur))
                client.send(cnt.to_bytes(2, byteorder='big'))
                for f in os.listdir(cur):
                    if path.isfile(path.join(cur, f)):
                        client.send(0x02.to_bytes(1, byteorder='big'))
                    else:
                        client.send(0x01.to_bytes(1, byteorder='big'))
                    name_enc = f.encode('utf-8')
                    client.send(len(name_enc).to_bytes(2, byteorder='big') + name_enc)
            elif flag == 3:
                print("Отправление файла")
                name_len = int.from_bytes(client.recv(2), byteorder='big')
                name = client.recv(name_len).decode('utf-8')
                try:
                    f = open(path.join(cur, name), 'rb')
                    file = f.read()
                    f.close()
                    client.send(0x82.to_bytes(1, byteorder='big') +
                                name_len.to_bytes(2, byteorder='big') +
                                name.encode('utf-8') +
                                len(file).to_bytes(4, byteorder='big') +
                                file)
                except:
                    client.send(0xC3.to_bytes(1, byteorder='big'))
            elif flag == 4:
                print("Переход к директории")
                name_len = int.from_bytes(client.recv(2), byteorder='big')
                name = client.recv(name_len).decode('utf-8')
                if path.exists(path.join(cur, name)) and path.isdir(path.join(cur, name)):
                    cur = path.join(cur, name)
                    client.send(0x81.to_bytes(1, byteorder='big'))
                else:
                    client.send(0xC4.to_bytes(1, byteorder='big'))
        except ConnectionResetError:
            print('Connection from {} was closed'.format(client.getpeername()))
            break


while True:
    # thread for clients
    client_socket, address_socket = server.accept()
    thread = threading.Thread(target=handle, args=[client_socket]).start()
