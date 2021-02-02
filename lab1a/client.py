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
my_nick = input("Your nickname: ")
nickname = my_nick.encode('ascii')
nickname_header = len(nickname).to_bytes(5, byteorder='big')
client.send(nickname_header + nickname)


def get_package(sock):
    header = sock.recv(5)
    length = int.from_bytes(header, byteorder='big', signed=False)
    return sock.recv(length).decode('ascii')


# converts from Greenwich time to local time
def local_time(time_of_message):
    timezone = -time.timezone / 3600
    time_of_message_format = datetime.datetime.strptime(time_of_message, "%H:%M")
    local_time_of_message = time_of_message_format + datetime.timedelta(hours=timezone)
    time_format = datetime.datetime.strftime(local_time_of_message, "%H:%M")
    return time_format


def receiving():
    while True:
        try:
            time_of_message = int.from_bytes(client.recv(4), byteorder='big', signed=False)
            time_of_message -= time.timezone
            nick = get_package(client)
            loc_time = datetime.datetime.fromtimestamp(time_of_message)
            message = get_package(client)

            print('<{}> [{}] {}'.format(loc_time.strftime('%H:%M:%S'), nick, message))

        except Exception as e:
            # Any other exception - something bad happened, exit
            print('Reading error: ' + str(e))
            client.close()
            sys.exit()


# send to server
def write():
    while True:
        message = input()

        if message:
            enc_nick = my_nick.encode('ascii')
            nick_header = len(enc_nick).to_bytes(5, byteorder='big')
            enc_message = message.encode('ascii')
            message_header = len(enc_message).to_bytes(5, byteorder='big')
            client.send(nick_header + enc_nick)
            client.send(message_header + enc_message)


# start thread for listening and sending
receive_thread = threading.Thread(target=receiving)
receive_thread.start()
# write()

write_thread = threading.Thread(target=write)
write_thread.start()
