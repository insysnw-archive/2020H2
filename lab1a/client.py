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

# choosing nickname
my_nick = input("Имя: ")
nickname = my_nick.encode('utf-8')
nickname_header = len(nickname).to_bytes(5, byteorder='big')


def get_package(sock):
    header = sock.recv(2)
    length = int.from_bytes(header, byteorder='big', signed=False)
    return sock.recv(length).decode('utf-8')


def receiving():
    while True:
        try:
            time_of_message = int.from_bytes(client.recv(4), byteorder='big', signed=False)
            time_of_message -= time.timezone
            loc_time = datetime.datetime.fromtimestamp(time_of_message)
            nick = get_package(client)
            message = get_package(client)
            print('{} | {}: {}'.format(loc_time.strftime('%H:%M:%S'), nick, message))

        except Exception as e:
            print('Reading error: ' + str(e))
            client.close()
            sys.exit()


# send to server
def write():
    while True:
        message = input()
        if message:
            enc_nick = my_nick.encode('utf-8')
            nick_len = len(enc_nick).to_bytes(2, byteorder='big')
            enc_message = message.encode('utf-8')
            message_len = len(enc_message).to_bytes(2, byteorder='big')
            client.send(nick_len + enc_nick)
            client.send(message_len + enc_message)


# start thread for listening and sending
receive_thread = threading.Thread(target=receiving)
receive_thread.start()
# write()

write_thread = threading.Thread(target=write)
write_thread.start()
