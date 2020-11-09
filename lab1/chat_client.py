import socket
import select
import errno
import sys
import os
import datetime
from _thread import start_new_thread

HEADER_LENGTH = 10

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

# Set connection to non-blocking state
client_socket.setblocking(False)

my_username = input("Username: ")
# Prepare username and header(username length) and send them
username = my_username.encode('utf-8')
username_header = f"{len(username):<{HEADER_LENGTH}}".encode('utf-8')
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
                username_length = int(username_header.decode('utf-8').strip())

                # Receive and decode username
                username = client_socket.recv(username_length).decode('utf-8')

                # Do the same for message
                message_header = client_socket.recv(HEADER_LENGTH)
                message_length = int(message_header.decode('utf-8').strip())
                message = client_socket.recv(message_length).decode('utf-8')

                # Print message
                curr_time=datetime.datetime.now().strftime('%H:%M')
                print(f'\r\033[K<{curr_time}> [{username}] {message}')
                sys.stdout.write("[You] ")
                sys.stdout.flush()


            except IOError as e:

        # This is normal on non blocking connections - when there are no incoming data error is going to be raised
        # Some operating systems will indicate that using AGAIN, and some using WOULDBLOCK error code
        # We are going to check for both
                if e.errno != errno.EAGAIN and e.errno != errno.EWOULDBLOCK:
                    print('Reading error: {}'.format(str(e)))
                    client_socket.close()
                    sys.exit()


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
        message_header = f"{len(enc_message):<{HEADER_LENGTH}}".encode('utf-8')
        client_socket.send(message_header + enc_message)

        curr_time=datetime.datetime.now().strftime('%H:%M')
        print(f'\033[1C\033[1A\r<{curr_time}> [You] {message}')
 