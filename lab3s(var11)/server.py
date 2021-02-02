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
prod = {}
nextId = 1
print(f"Server Started!")


# receive messages from clients
def receive_prod(client):
    while True:
        try:
            cnt = client.recv(2)
            price = client.recv(4)
            name_len = client.recv(2)
            length = int.from_bytes(name_len, byteorder='big')
            name = client.recv(length)
        except ConnectionResetError:
            print("Connection reset")
            return dict()

        return {'cnt': cnt, 'price': price, 'name_len': name_len, 'name': name}


def handle(client):
    global nextId
    while True:
        try:
            flag = int.from_bytes(client.recv(1), byteorder='big')
            if flag == 1:
                print("Запрос на получение информации о товарах")
                client.send(0x82.to_bytes(1, byteorder='big') + len(prod).to_bytes(2, byteorder='big'))
                for p in prod.keys():
                    client.send(p.to_bytes(4, byteorder='big') + prod[p]['cnt'] +
                                prod[p]['price'] + prod[p]['name_len'] + prod[p]['name'])
            elif flag == 2:
                print("Запрос на добавление товара")
                p = receive_prod(client)
                f_id = 0
                for p_id in prod.keys():
                    if prod[p_id]['name'] == p['name']:
                        if prod[p_id]['price'] != p['price']:
                            f_id = -1
                        else:
                            f_id = p_id
                        break
                if f_id == 0:
                    prod[nextId] = p
                    nextId += 1
                    client.send(0x81.to_bytes(1, byteorder='big'))
                elif f_id == -1:
                    client.send(0xC2.to_bytes(1, byteorder='big'))
                else:
                    cnt = int.from_bytes(prod[f_id]['cnt'], byteorder='big')
                    p_cnt = int.from_bytes(p['cnt'], byteorder='big')
                    cnt += p_cnt
                    prod[f_id]['cnt'] = cnt.to_bytes(2, byteorder='big')
                    client.send(0x81.to_bytes(1, byteorder='big'))
            elif flag == 3:
                print("Запрос на покупку товара")
                cnt = client.recv(2)
                prod_id = int.from_bytes(client.recv(4), byteorder='big')
                try:
                    if int.from_bytes(prod[prod_id]['cnt'], byteorder='big') < int.from_bytes(cnt, byteorder='big'):
                        client.send(0xC3.to_bytes(1, byteorder='big'))
                    else:
                        p_cnt = int.from_bytes(prod[prod_id]['cnt'], byteorder='big')
                        p_cnt -= int.from_bytes(cnt, byteorder='big')
                        prod[prod_id]['cnt'] = p_cnt.to_bytes(2, byteorder='big')
                        client.send(0x81.to_bytes(1, byteorder='big'))
                except KeyError:
                    client.send(0xC3.to_bytes(1, byteorder='big'))
        except ConnectionResetError:
            print('Connection from {} was closed'.format(client.getpeername()))
            break


while True:
    # thread for clients
    client_socket, address_socket = server.accept()
    thread = threading.Thread(target=handle, args=[client_socket]).start()
