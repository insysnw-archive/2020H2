import socket
import threading

# port for listening
PORT = 1234

# IPv4 address for server
SERVER = socket.gethostbyname(socket.gethostname())

ADDRESS = (SERVER, PORT)

# format for decoding
FORMAT = "utf-8"

# lists that will contain all the client sockets connected to the server and client's names.
client_sockets, names = [], []

# create a new socket for the server with IPv4 (AF_INET) and TCP (SOCK_STREAM)
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# bind the address of the server to the socket
server.bind(ADDRESS)
print("server is working on address: " + SERVER + ", port: " + str(PORT))

# listening for connections
server.listen()


# method to handle the incoming messages
def handle(connection, connection_address, connection_name):
    print(f"new connection: {connection_address}, name: {connection_name}")

    while True:
        # receive message header
        message = connection.recv(1024)
        if len(message) == 0:
            break
        broadcast_message(message)

    connection.shutdown(socket.SHUT_RDWR)
    connection.close()
    client_sockets.remove(connection)
    names.remove(connection_name)


# method for broadcasting messages to each of the clients
def broadcast_message(message):
    for client in client_sockets:
        client.send(message)


while True:
    # accept connection (returns a new socket to the client and the address bound to it)
    client_socket, address = server.accept()
    client_socket.send("NAME".encode(FORMAT))

    # 1024 represents the max amount of data that can be received in bytes
    name = client_socket.recv(1024).decode(FORMAT)

    # append the name and client to the respective lists
    names.append(name)
    client_sockets.append(client_socket)

    broadcast_message(f"\n\n{name} has joined the chat!".encode(FORMAT))

    # start the handling thread
    thread = threading.Thread(target=handle, args=(client_socket, address, name))
    thread.start()

    # no. of clients connected to the server
    print(f"active connections: {threading.activeCount() - 1}")
