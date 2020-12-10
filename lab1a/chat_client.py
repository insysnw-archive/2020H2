import socket
import errno
import sys
import os
import datetime
from _thread import start_new_thread

HEADER_LENGTH = 5

TIME_LENGTH = 4

if(len(sys.argv)!=3):
    print("USAGE: python chat_client.py [Server IP] [Server port]")
    sys.exit()

IP = sys.argv[1]
PORT = int(sys.argv[2])

# Needed for enabling ANSI escape characters on Windows 10
os.system('')

# Create a socket
# socket.AF_INET - address family, IPv4
# socket.SOCK_STREAM - TCP, conection-based
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Connect to a given ip and port
client_socket.connect((IP, PORT))

# Set connection to blocking state
client_socket.setblocking(True)

my_username = input("Username: ")
# Prepare username and header(username length) and send them
username = my_username.encode('utf-8')
if(len(username)>=pow(2,8*HEADER_LENGTH)):
    username=username[:pow(2,8*HEADER_LENGTH)-1]
username_header = len(username).to_bytes(HEADER_LENGTH, byteorder='big')
client_socket.send(username_header + username)
print("Yay, successfull, you can now start messaging")

def message_update():

        # Loop over received messages(may be more than one)
        while True:
            # Receive our "header" with username length
            try:
                username_header = client_socket.recv(HEADER_LENGTH)

                # If we received no data, server gracefully closed a connection
                if not len(username_header):
                    print('\r\033[KConnection closed by the server')
                    client_socket.close()
                    sys.exit()

                # Convert header to int value
                username_length = int.from_bytes(username_header,byteorder='big',signed=False)
                

                # Receive and decode username
                username = client_socket.recv(username_length).decode('utf-8')

                # Receive and decode time
                message_time_enc = client_socket.recv(TIME_LENGTH)
                message_time=datetime.datetime.fromtimestamp(
                    int.from_bytes(message_time_enc,byteorder='big',signed=False))

                # Receive and decode message
                message_header = client_socket.recv(HEADER_LENGTH)
                message_length = int.from_bytes(message_header,byteorder='big',signed=False)
                message = client_socket.recv(message_length).decode('utf-8')

                # Print message
                message_time=message_time.strftime('%H:%M')
                print(f'\r\033[K<{message_time}> [{username}] {message}')
                sys.stdout.write("[You] ")
                sys.stdout.flush()

            except Exception as e:
                # Any other exception - something bad happened, exit
                print('Reading error: '.format(str(e)))
                client_socket.close()
                sys.exit()

# To be able to write and receive messages at the same time, we will start message_update() in a separate thread
start_new_thread(message_update,())


while True:

    message = input("[You] ")

    # If message is not empty - send it
    if message:

        enc_message = message.encode('utf-8')
        if(len(enc_message)>=pow(2,8*HEADER_LENGTH)):
            enc_message=enc_message[:pow(2,8*HEADER_LENGTH)-1]
        message_header = len(enc_message).to_bytes(HEADER_LENGTH, byteorder='big')
        client_socket.send(message_header + enc_message)

        curr_time=datetime.datetime.now().strftime('%H:%M')
        print(f'\033[1C\033[1A\r<{curr_time}> [You] {message}')