import socket
import argparse
import threading


# opcodes
NAM = 1  # initial username message / login 
MES = 2  # message data 
PER = 3  # permisson from server to send messages to chat / connection successful
END = 4  # signal to end connetcion

MSG_SIZE = 1024  # message size

FORMAT = "utf-8"

# lists that will contain all the client sockets connected to the server and client's names.
client_sockets, client_names = [], []


# send message to each client
def send_to_clietns(message):
    for client in client_sockets:
        client.send(message)

# method to handle the incoming messages
def handle_client(connection, connection_address):
    client_name = None

    while True:
        msg = connection.recv(MSG_SIZE)
        if len(msg) == 0:
            break

        elif msg[0] == END:
            connection.send(bytes([END]))
            break

        elif msg[0] == NAM:
            client_name = msg[1:].decode(FORMAT)

            print(f"new client: {connection_address}, name: {client_name}")

            # append the name and client_socket to the respective lists
            client_names.append(client_name)
            client_sockets.append(connection)

            msg = bytes([PER]) + msg[1:] + bytearray(' has joined chat!'.encode(FORMAT))
            send_to_clietns(msg)

        elif msg[0] == MES:
            reply = bytes([MES]) + client_name.encode(FORMAT) + bytes([0]) + msg[1:] + bytes([0])
            send_to_clietns(reply)

    connection.shutdown(socket.SHUT_RDWR)
    connection.close()
    client_sockets.remove(connection)
    client_names.remove(client_name)
    print(client_name, 'disconnected')


# command line arguments parser
parser = argparse.ArgumentParser(description='Chat server')
parser.add_argument("--ip", default='0.0.0.0', type=str, help='server IP address')
parser.add_argument("--port", default=1234, type=int, help='server port')
args = parser.parse_args()
server_ip = args.ip
server_port = args.port

server_address = (server_ip, server_port)
print('Server is starting at', server_ip,':', server_port)

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind(server_address)
server_socket.listen() 

try:
    while True:
        # accept connection (returns a new socket to the client and the address bound to it)
        connection, address = server_socket.accept()  # connection == socket for client connection

        # start the handling thread
        client_thread = threading.Thread(target=handle_client, args=(connection, address))
        client_thread.start()

except KeyboardInterrupt:
    pass
finally:
    server_socket.shutdown(socket.SHUT_RDWR)
    server_socket.close()
    print("\nserver socket closed")
    