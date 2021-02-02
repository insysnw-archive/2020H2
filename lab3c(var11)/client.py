import socket
import sys
import threading
import time
import datetime

if len(sys.argv) != 3:
    print("Select IP and server port!")
    sys.exit()

server = (sys.argv[1], int(sys.argv[2]))

# connect to server
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(server)


def receive_prod(client):
    while True:
        try:
            p_id = client.recv(4)
            cnt = client.recv(2)
            price = client.recv(4)
            name_len = client.recv(2)
            length = int.from_bytes(name_len, byteorder='big')
            name = client.recv(length)
        except ConnectionResetError:
            print("Connection reset")
            return dict()

        return {'id': p_id, 'cnt': cnt, 'price': price, 'name_len': name_len, 'name': name}


def receiving():
    while True:
        try:
            flag = int.from_bytes(client.recv(1), byteorder='big')
            if flag == 0x81:
                print("Successful")
            elif flag == 0x82:
                print("Информация о товарах")
                prod_cnt = int.from_bytes(client.recv(2), byteorder='big')
                print("Кол-во товаров: {}".format(prod_cnt))
                for i in range(prod_cnt):
                    prod = receive_prod(client)
                    print("ID товара: {}\n\tНазвание: {}\n\tКол-во: {}\n\tЦена: {}".format(
                        int.from_bytes(prod['id'], byteorder='big'),
                        prod['name'].decode('ascii'),
                        int.from_bytes(prod['cnt'], byteorder='big'),
                        int.from_bytes(prod['price'], byteorder='big')
                    ))
            elif flag == 0xC2:
                print("Ошибка добавления")
            elif flag == 0xC3:
                print("Ошибка покупки")
        except Exception as e:
            # Any other exception - something bad happened, exit
            print('Reading error: ' + str(e))
            client.close()
            sys.exit()


# send to server
def write():
    while True:
        print("1 - Получить информацию\n2 - Добваить товар\n3 - Купить товар")
        try:
            message = int(input("Действие: "))
            if message == 1:
                client.send(0x01.to_bytes(1, byteorder='big'))
            elif message == 2:
                print("Добавление товара")
                name = input("Имя товара: ")
                cnt = int(input("Кол-во товара: "))
                price = int(input("Цена товара: "))
                name_len = len(name).to_bytes(2, byteorder='big')
                client.send(0x02.to_bytes(1, byteorder='big') +
                            cnt.to_bytes(2, byteorder='big') +
                            price.to_bytes(4, byteorder='big') +
                            name_len + name.encode('ascii'))
            elif message == 3:
                print("Покупка товара")
                p_id = int(input("ID товара: "))
                cnt = int(input("Кол-во товара: "))
                client.send(0x03.to_bytes(1, byteorder='big') +
                            cnt.to_bytes(2, byteorder='big') +
                            p_id.to_bytes(4, byteorder='big'))
        except ValueError:
            print("Неверный ввод")
        time.sleep(1)


# start thread for listening and sending
receive_thread = threading.Thread(target=receiving)
receive_thread.start()
# write()

write_thread = threading.Thread(target=write)
write_thread.start()
