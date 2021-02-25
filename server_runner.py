import sys

from src.lab3.email.server import Server

if __name__ == '__main__':
    address = sys.argv[1]
    port = int(sys.argv[2])

    server = Server(address, port)
    server.start()
