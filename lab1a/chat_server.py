import socket
import datetime
import math
from _thread import start_new_thread

HEADER_LENGTH = 5

TIME_LENGTH = 4

IP = "0.0.0.0"
PORT = 1234

# Create a socket
# socket.AF_INET - address family, IPv4
# socket.SOCK_STREAM - TCP, conection-based
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Sets REUSEADDR (as a socket option) to 1 on socket
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

server_socket.bind((IP, PORT))

server_socket.setblocking(True)

# Listen to new connections
server_socket.listen()

# List of sockets
sockets_list = [server_socket]

# List of connected clients
clients = {}

print(f'Listening for connections on {IP}:{PORT}...')

# Handles message receiving
def receive_message(client_socket):

    # Receive our "header" with message length
    try:
        message_header = client_socket.recv(HEADER_LENGTH)
    except ConnectionResetError:
        return False
        
    # If we received no data, client gracefully closed a connection
    if not len(message_header):
        return False
        # Convert header to int value
    message_length = int.from_bytes(message_header,byteorder='big',signed=False)

        # Return an object of message header and message data
    return {'header': message_header, 'data': client_socket.recv(message_length)}

def handle_connection(client_socket,client_address):

    #New user
    if client_socket not in sockets_list:
        # Client should send his name right away, receive it
        user = receive_message(client_socket)

        # If False - client disconnected before he sent his name
        if user is False:
            return

        # Also save username and username header
        clients[client_socket] = user

        # Add accepted socket to socket_list
        sockets_list.append(client_socket)

        print('Accepted new connection from {}:{}, username: {}'.format(*client_address, user['data'].decode('utf-8')))
        message=f"User {user['data'].decode('utf-8')} entered the channel".encode('utf-8')
        if(len(message)>=pow(2, HEADER_LENGTH)):
            message=message[:pow(2, HEADER_LENGTH)-1]
        message_header=len(message).to_bytes(HEADER_LENGTH, byteorder='big')

        curr_time=math.floor(datetime.datetime.now().timestamp()).to_bytes(TIME_LENGTH, byteorder='big')
        for tmp_socket in sockets_list:

            # But don't sent it to sender
            if tmp_socket != client_socket and tmp_socket != server_socket:
                tmp_socket.send(user['header'] + user['data'] +curr_time + message_header + message)

    while True:
        message = receive_message(client_socket)

        # If False, client disconnected
        if message is False:

            user = clients[client_socket]

            print('Closed connection from: {}'.format(clients[client_socket]['data'].decode('utf-8')))
            message=f"User {user['data'].decode('utf-8')} left the channel".encode('utf-8')
            if(len(message)>=pow(2,8*HEADER_LENGTH)):
                message=message[:pow(2,8*HEADER_LENGTH)-1]
            message_header=len(message).to_bytes(HEADER_LENGTH, byteorder='big')

            curr_time=math.floor(datetime.datetime.now().timestamp()).to_bytes(TIME_LENGTH, byteorder='big')
            for socket in sockets_list:

                # But don't sent it to sender
                if socket != client_socket and socket != server_socket:
                    socket.send(user['header'] + user['data'] + curr_time + message_header + message)

            # Remove from list for socket.socket()
            sockets_list.remove(client_socket)

            # Remove from list of users
            del clients[client_socket]
            break

        # Get user by socket
        user = clients[client_socket]

        print(f'Received message from {user["data"].decode("utf-8")}: {message["data"].decode("utf-8")}')

        curr_time=math.floor(datetime.datetime.now().timestamp()).to_bytes(TIME_LENGTH, byteorder='big')
        # Iterate over connected clients and broadcast message
        for socket in sockets_list:

            # But don't sent it to sender
            if socket != client_socket and socket != server_socket:

                # Send user and message (both with their headers)
                socket.send(user['header'] + user['data'] + curr_time + message['header'] + message['data'])

while True:

    # Accept new connection
    client_socket, client_address = server_socket.accept()

    client_socket.setblocking(True)

    start_new_thread(handle_connection,(client_socket,client_address))