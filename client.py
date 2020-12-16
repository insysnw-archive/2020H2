import socket
import sys
import threading
import time

if len(sys.argv) != 3:
    print("Select IP and server port!")
    sys.exit()

server = (sys.argv[1], int(sys.argv[2]))

# connect to server
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(server)

# choosing nickname
my_nick = input("Your nickname: ")
nickname = my_nick.encode('ascii')
nickname_header = len(nickname).to_bytes(5, byteorder='big')
client.send(nickname_header + nickname)


def get_package(sock):
    header = sock.recv(5)
    length = int.from_bytes(header, byteorder='big', signed=False)

    return sock.recv(length).decode('ascii')


def receiving():
    while True:
        try:
            nick = get_package(client)
            message = get_package(client)

            print('<{}> [{}] {}'.format(time.strftime('%H:%M', time.localtime()), nick, message))

        except Exception as e:
            # Any other exception - something bad happened, exit
            print('Reading error: '.format(str(e)))
            client.close()
            sys.exit()


# send to server
def write():
    while True:
        message = input()

        if message:
            enc_message = message.encode('ascii')
            message_header = len(enc_message).to_bytes(5, byteorder='big')
            client.send(message_header + enc_message)
            print('<{}> [{}] {}'.format(time.strftime('%H:%M', time.localtime()), my_nick, message))


# start thread for listening and sending
receive_thread = threading.Thread(target=receiving)
receive_thread.start()
#write()

write_thread = threading.Thread(target=write)
write_thread.start()
