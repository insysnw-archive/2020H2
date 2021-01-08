import errno
import socket
import sys
import threading
import time
import os

if len(sys.argv) != 3:
    print("Select IP and server port!")
    sys.exit()

client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect((sys.argv[1], int(sys.argv[2])))
client_socket.setblocking(False)

my_nick = input("Your nickname: ")
nickname = my_nick.encode('ascii')
nickname_header = len(nickname).to_bytes(5, byteorder='big')
client_socket.send(nickname_header + nickname)


def get_package(sock):
    header = sock.recv(5)

    if not len(header):
        print('Connection was closed by server')
        client_socket.close()
        os._exit(0)

    length = int.from_bytes(header, byteorder='big', signed=False)

    return sock.recv(length).decode('ascii')


def receiving():
    while True:
        try:
            nick = get_package(client_socket)
            message = get_package(client_socket)
            print('<{}> [{}] {}'.format(time.strftime('%H:%M', time.localtime()), nick, message))

        # for blocking sockets
        except IOError as e:
            if e.errno != errno.EAGAIN and e.errno != errno.EWOULDBLOCK:
                print('Reading error: {}'.format(str(e)))
                client_socket.close()
                sys.exit()


def sending():
    while True:
        try:
            message = input()

            if message:
                enc_message = message.encode('ascii')
                message_header = len(enc_message).to_bytes(5, byteorder='big')
                client_socket.send(message_header + enc_message)
                # print('<{}> [{}] {}'.format(time.strftime('%H:%M', time.localtime()), my_nick, message))
        except KeyboardInterrupt:
            os._exit(0)


receive_thread = threading.Thread(target=receiving)
receive_thread.daemon = True
receive_thread.start()

sending()
