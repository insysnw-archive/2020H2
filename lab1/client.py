import socket
import sys
import threading
import time

if len(sys.argv) != 3:
    print("Specify IP and port")
    sys.exit()

address = (sys.argv[1], int(sys.argv[2]))
my_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
my_socket.connect(address)

my_nick = input("Your nickname: ")
nickname = my_nick.encode()
nickname_header = len(nickname).to_bytes(5, byteorder='big')
my_socket.send(nickname_header + nickname)


def get_package(sock):
    header = sock.recv(5)
    if not len(header):
        print("The connection to the server has been dropped")
        my_socket.close()
        sys.exit()
    size = int.from_bytes(header, byteorder='big', signed=False)
    data = sock.recv(size).decode('ascii')
    if not len(data):
        print("The connection to the server has been dropped")
        my_socket.close()
        sys.exit()
    return data


def get_message():
    while True:
        msg = input()

        if msg:
            message = msg.encode()
            message_header = len(message).to_bytes(5, byteorder='big')
            my_socket.send(message_header + message)


receive_thread = threading.Thread(target=get_message, daemon=True).start()

while True:
    try:
        nick = get_package(my_socket)
        msg = get_package(my_socket)

        print('<{}> [{}] {}'.format(time.strftime('%H:%M', time.localtime()), nick, msg))

    except Exception as e:
        print("Exception: " + str(e))
        my_socket.close()
        sys.exit()
