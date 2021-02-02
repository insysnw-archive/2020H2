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


def type_dec(type):
    if type == 1:
        return "Директория"
    else:
        return "Файл"


def receiving():
    while True:
        try:
            flag = int.from_bytes(client.recv(1), byteorder='big')
            if flag == 0x81:
                print("Successful")
            elif flag == 0x82:
                print("Получение файла")
                name_len = int.from_bytes(client.recv(2), byteorder='big')
                name_enc = client.recv(name_len)
                name = name_enc.decode('utf-8')
                file_len = int.from_bytes(client.recv(4), byteorder='big')
                file = client.recv(file_len)
                print("Получен файл: {}".format(name))
                f = open(name, 'wb')
                f.write(file)
                f.close()
            elif flag == 0x83:
                print("Вывод каталога")
                cnt = int.from_bytes(client.recv(2), byteorder='big')
                for i in range(cnt):
                    type = int.from_bytes(client.recv(1), byteorder='big')
                    name_len = int.from_bytes(client.recv(2), byteorder='big')
                    name_enc = client.recv(name_len)
                    name = name_enc.decode('utf-8')
                    print("{}: {}".format(type_dec(type), name))
            elif flag == 0xC1:
                print("Ошибка получения")
            elif flag == 0xC2:
                print("Ошибка просмотра")
            elif flag == 0xC3:
                print("Ошибка отправления")
            elif flag == 0xC4:
                print("Ошибка перехода")
        except Exception as e:
            # Any other exception - something bad happened, exit
            print('Reading error: ' + str(e))
            client.close()
            sys.exit()


# send to server
def write():
    while True:
        print("1 - Отправка файла\n2 - Просмотр каталога\n3 - Получение файла\n4 - Переход к директории")
        try:
            message = int(input("Действие: "))
            if message == 1:
                print("Отправка файла")
                name = input("Имя файла: ")
                f = open(name, 'rb')
                file = f.read()
                client.send(0x01.to_bytes(1, byteorder='big'))
                client.send(len(name.encode('utf-8')).to_bytes(2, byteorder='big') +
                            name.encode('utf-8') +
                            len(file).to_bytes(4, byteorder='big') +
                            file)
            elif message == 2:
                print("Просмотр директории")
                client.send(0x02.to_bytes(1, byteorder='big'))
            elif message == 3:
                print("Получение файла")
                name = input("Имя файла: ")
                name_enc = name.encode('utf-8')
                client.send(0x03.to_bytes(1, byteorder='big') +
                            len(name_enc).to_bytes(2, byteorder='big') + name_enc)
            elif message == 4:
                print("Переход к директории")
                directory = input("Директория: ")
                dir_enc = directory.encode('utf-8')
                client.send(0x04.to_bytes(1, byteorder='big') +
                            len(dir_enc).to_bytes(2, byteorder='big') + dir_enc)
        except ValueError:
            print("Неверный ввод")
        except FileNotFoundError:
            print("Файл не найден")
        time.sleep(1)


# start thread for listening and sending
receive_thread = threading.Thread(target=receiving)
receive_thread.start()
# write()

write_thread = threading.Thread(target=write)
write_thread.start()
