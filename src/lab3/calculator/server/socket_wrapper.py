import socket

from ..protocol import (Operation, Response)


class SocketWrapper:
    def __init__(self, skt: socket, is_server=False):
        self.skt = skt
        self.is_server = is_server

    def accept(self):
        return self.skt.accept()

    def recv(self) -> Operation:
        try:
            byte_arr = self.skt.recv(16)
        except ConnectionResetError:
            return None

        return Operation.unpack(byte_arr)

    def send(self, response: Response):
        print(f'Result: {response.__dict__}')
        self.skt.send(Response.pack(response))
