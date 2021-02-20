import socket
import argparse
import threading
import datetime
import sys


# opcodes
NAM = 1  # initial username message / login
MES = 2  # message data
PER = 3  # permisson from server to send messages to chat / connection successful
END = 4  # signal to end connetcion

MSG_SIZE = 1024  # message size

FORMAT = "utf-8"

working = True

def handle_incoming():
    global working
    while True:
        msg =  client_socket.recv(MSG_SIZE)
        if len(msg) == 0 or msg[0] == END:
            print('Connection with server ended. If client is still running, press Enter')
            working = False
            break

        elif msg[0] == PER:
            print(msg[1:].decode(FORMAT))

        elif msg[0] == MES:
            name = bytearray()
            i = 1
            while msg[i] != 0:  # read until first zero-byte - name
                name.append(msg[i])
                i += 1

            message = bytearray()
            i += 1
            while msg[i] != 0:  # read until second zero-byte - message
                message.append(msg[i])
                i += 1

            now = datetime.datetime.now()
            time = now.strftime('%H:%M')

            output = '<' + time + '>' + '[' + name.decode(FORMAT) + ']' + ': ' + message.decode(FORMAT)
            print(output)


# command line arguments parser
parser = argparse.ArgumentParser(description='Chat client')
parser.add_argument("--ip", default='127.0.0.1', type=str, help='server IP address')
parser.add_argument("--port", default=1234, type=int, help='server port')
args = parser.parse_args()
server_ip = args.ip
server_port = args.port

server_address = (server_ip, server_port)
print('Server address', server_ip,':', server_port)

# create a new client socket
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# connect to the server
client_socket.connect(server_address)


# send name to server to connect to chat
print('Enter your name:')
name = str(input())
msg = bytes([NAM]) + bytearray(name.encode(FORMAT))
client_socket.send(msg)

msg = client_socket.recv(MSG_SIZE)

if msg[0] == PER:
    print(msg[1:].decode(FORMAT))
    reading_thread = threading.Thread(target=handle_incoming)
    reading_thread.start()

    while True:
        msg = sys.stdin.readline()

        if not working:
            break

        if msg == '!exit!\n':
            print('Ending connection with server')
            client_socket.send(bytes([END]))
            break

        else:
            client_socket.send(bytes([MES]) + msg.encode(FORMAT))

reading_thread.join()
client_socket.shutdown(socket.SHUT_RDWR)
client_socket.close()
