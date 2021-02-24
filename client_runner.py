import sys

from src.lab3.elmail.client import Client

if __name__ == '__main__':
    address = sys.argv[1]
    port = int(sys.argv[2])

    client = Client(address, port)
    client.start()

