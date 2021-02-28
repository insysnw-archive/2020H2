import socket
import threading
import time
import sys

# check args
if len(sys.argv) != 3:
    print("Select IP and server port")
    sys.exit()
server = (sys.argv[1], int(sys.argv[2]))

# connection
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(server)

# enter nickname
nickname = input("Your nickname: ")
_nickname = nickname.encode('ascii')
nickname_header = len(_nickname).to_bytes(5, byteorder='big')
client.send(nickname_header + _nickname)


def get_package(_client):
    length = int.from_bytes(_client.recv(5), byteorder='big', signed=False)
    return _client.recv(length).decode('ascii')


# server listener
def receive():
    while True:
        try:
            # receive and print localtime+nick+message
            nick = get_package(client)
            message = get_package(client)
            print('<{}> [{}] {}'.format(time.strftime('%H:%M', time.localtime()), nick, message))
        except:
            # connection closed on error
            print("An error occured!")
            client.close()
            sys.exit()


# write to server
def write():
    while True:
        message = input()
        if message:
            _message = message.encode('ascii')
            message_header = len(_message).to_bytes(5, byteorder='big')
            client.send(message_header + _message)
            print('<{}> [{}] {}'.format(time.strftime('%H:%M', time.localtime()), nickname, message))


# threads starting
receive_thread = threading.Thread(target=receive)
receive_thread.start()

write_thread = threading.Thread(target=write)
write_thread.start()
