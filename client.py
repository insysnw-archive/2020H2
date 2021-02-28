import socket
import argparse
import threading
import datetime
import sys
import errno

# opcodes
TYPE_INIT = 1
TYPE_DATA = 2
TYPE_OKNAME = 3
TYPE_DUP = 4
TYPE_END = 5
TYPE_LEN = 6
MSG_SIZE = 1024  # message size

FORMAT = "utf-8"


working = True
check = False


def handle_incoming():
    global working, msg, check
    while True:
        msg = client_socket.recv(MSG_SIZE)

        if len(msg) == 0 or msg[0] == TYPE_END:
            print("Соединение с сервером разорвано")
            working = False
            break
        elif msg[0] == TYPE_OKNAME:
            print(msg[1:].decode(FORMAT))
            check = True

        elif msg[0] == TYPE_DUP:
            print("Дублирование никнейма")

        elif msg[0] == TYPE_LEN:
            print("Имя не соответствует параметрам")

        elif msg[0] == TYPE_DATA:
            name = bytearray()
            i = 1
            while msg[i] != 0:  # read until first zero-byte - name
                name.append(msg[i])
                i += 1
            message = bytearray()
            i += 1
            while msg[i] != 0:  # read until second zero-byte - message
                message.append(msg[i])
                i += 1
            now = datetime.datetime.now()
            time = now.strftime('%H:%M')
            output = ' ' + time + ' [' + name.decode(FORMAT) + ']' + ': ' + message.decode(FORMAT)
            print(output)


# command line arguments parser
parser = argparse.ArgumentParser(description='Chat client')
parser.add_argument("--ip", default='127.0.0.1', type=str, help='server IP address')
parser.add_argument("--port", default=1234, type=int, help='server port')
args = parser.parse_args()
server_ip = args.ip
server_port = args.port

server_address = (server_ip, server_port)
print('Server address', server_ip, ':', server_port)
print('Для выключения чата введите close chat')
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect(server_address)
client_socket.setblocking(True)

# send name to server to connect to chat
print('Ник:')
name = str(input())
msg = bytes([TYPE_INIT]) + bytearray(name.encode(FORMAT))
client_socket.send(msg)

reading_thread = threading.Thread(target=handle_incoming)
reading_thread.start()

while True:
    msg = sys.stdin.readline()

    if not working:
        break
    if check:
        if msg == 'close chat\n':
            print('Ending connection with server')
            client_socket.send(bytes([TYPE_END]))
            break
        else:
            client_socket.send(bytes([TYPE_DATA]) + msg.encode(FORMAT))
    else:
        client_socket.send(bytes([TYPE_INIT]) + msg.encode(FORMAT))

reading_thread.join()
client_socket.shutdown(socket.SHUT_RDWR)
client_socket.close()
