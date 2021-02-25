import socket
import sys
import threading
from datetime import datetime
from time import timezone

if len(sys.argv) != 3:
    server = ("localhost", 2020)
else:
    server = (sys.argv[1], int(sys.argv[2]))

client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(server)


def read_block():
    data_len = int.from_bytes(client.recv(2), byteorder='big')
    data = client.recv(data_len).decode('utf-8')
    return data


def receive():
    while True:
        try:
            flag = int.from_bytes(client.recv(1), byteorder='big')
            if flag == 0x81:
                print("Успешно")
            elif flag == 0x82:
                name = read_block()
                msg = read_block()
                utime = int.from_bytes(client.recv(4), byteorder='big')
                utime -= timezone
                local_time = datetime.fromtimestamp(utime)
                print("{} | Получено сообщение от {}: {}".format(
                    local_time.strftime('%H:%M'),
                    name, msg))
            elif flag == 0xC1:
                print("Ошибка авторизации")
            elif flag == 0xC2:
                print("Ошибка отправки")
            elif flag == 0xC3:
                print("Ошибка рассылки")
        except Exception as e:
            print('Exception: ' + str(e))
            client.close()
            sys.exit()


def block_of(data):
    data_enc = data.encode('utf-8')
    return len(data_enc).to_bytes(2, byteorder='big') + data_enc


def main_cycle():
    print("1 - Авторизация\n2 - Отправка сообщения\n3 - Рассылка сообщения\n4 - Выход")
    while True:
        try:
            action = int(input())
            if action == 1:
                print("Авторизация")
                name = input("Имя: ")
                client.send(b'\x01' + block_of(name))
            elif action == 2:
                print("Отправка сообщения")
                name = input("Имя: ")
                msg = input("Сообщение: ")
                client.send(b'\x02' +
                            block_of(name) +
                            block_of(msg))
            elif action == 3:
                print("Рассылка сообщения")
                msg = input("Сообщение: ")
                client.send(b'\x03' + block_of(msg))
            elif action == 4:
                print("Выход")
                client.close()
                sys.exit(0)
        except ValueError:
            print("Неверный ввод")


receive_thread = threading.Thread(target=receive)
receive_thread.start()
main_thread = threading.Thread(target=main_cycle)
main_thread.start()
