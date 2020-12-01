import socket
import select

HEADER_LENGTH = 5

IP = "0.0.0.0"
PORT = 1234

# Create a socket
# socket.AF_INET - address family, IPv4
# socket.SOCK_STREAM - TCP, conection-based
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Sets REUSEADDR (as a socket option) to 1 on socket
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

server_socket.bind((IP, PORT))

# Listen to new connections
server_socket.listen()

# List of sockets for select.select()
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

while True:

    # Calls Unix select() system call or Windows select() WinSock call with three parameters:
    #   - rlist - sockets to be monitored for incoming data
    #   - wlist - sockets for data to be send to
    #   - xlist - sockets to be monitored for exceptions (we want to monitor all sockets for errors)
    # Returns lists:
    #   - reading - sockets we received some data on (that way we don't have to check sockets manually)
    #   - writing - sockets ready for data to be send thru them
    #   - errors  - sockets with some exceptions
    read_sockets, _, exception_sockets = select.select(sockets_list, [], sockets_list)


    # Iterate over sockets
    for some_socket in read_sockets:

        # If socket is a server socket - new connection, accept it
        if some_socket == server_socket:

            # Accept new connection
            client_socket, client_address = server_socket.accept()

            # Client should send his name right away, receive it
            user = receive_message(client_socket)

            # If False - client disconnected before he sent his name
            if user is False:
                continue

            # Add accepted socket to select.select() list
            sockets_list.append(client_socket)

            # Also save username and username header
            clients[client_socket] = user

            print('Accepted new connection from {}:{}, username: {}'.format(*client_address, user['data'].decode('utf-8')))
            message=f"User {user['data'].decode('utf-8')} entered the channel".encode('utf-8')
            if(len(message)>=pow(2,8*HEADER_LENGTH)):
                message=message[:pow(2,8*HEADER_LENGTH)-1]
            message_header=len(message).to_bytes(HEADER_LENGTH, byteorder='big')
            for tmp_socket in clients:

                # But don't sent it to sender
                if tmp_socket != client_socket:
                    tmp_socket.send(user['header'] + user['data'] + message_header + message)

        # Else existing socket is sending a message
        else:

            message = receive_message(some_socket)

            # If False, client disconnected
            if message is False:

                user = clients[some_socket]

                print('Closed connection from: {}'.format(clients[some_socket]['data'].decode('utf-8')))
                message=f"User {user['data'].decode('utf-8')} leaved the channel".encode('utf-8')
                if(len(message)>=pow(2,8*HEADER_LENGTH)):
                    message=message[:pow(2,8*HEADER_LENGTH)-1]
                message_header=len(message).to_bytes(HEADER_LENGTH, byteorder='big')
                for client_socket in clients:

                    # But don't sent it to sender
                    if client_socket != some_socket:
                        client_socket.send(user['header'] + user['data'] + message_header + message)

                # Remove from list for socket.socket()
                sockets_list.remove(some_socket)

                # Remove from list of users
                del clients[some_socket]
                continue

            # Get user by socket
            user = clients[some_socket]

            print(f'Received message from {user["data"].decode("utf-8")}: {message["data"].decode("utf-8")}')

            # Iterate over connected clients and broadcast message
            for client_socket in clients:

                # But don't sent it to sender
                if client_socket != some_socket:

                    # Send user and message (both with their headers)
                    client_socket.send(user['header'] + user['data'] + message['header'] + message['data'])

    # Handle some socket exceptions just in case
    for some_socket in exception_sockets:

        # Remove from list for socket.socket()
        sockets_list.remove(some_socket)

        # Remove from our list of users
        del clients[some_socket]