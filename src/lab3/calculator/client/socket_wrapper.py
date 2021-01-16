import socket

from ..protocol import (Operation, Response)


class SocketWrapper:
    def __init__(self, skt: socket, is_server=False):
        self.skt: socket = skt
        self.is_server = is_server

    def accept(self):
        client_socket, client_address = self.skt.accept()
        return SocketWrapper(client_socket), client_address

    def recv(self, timeout=None) -> Response:
        self.skt.settimeout(timeout)
        try:
            byte_arr = self.skt.recv(52)
        except ConnectionResetError:
            return None
        except socket.timeout:
            return Response(-1, -1, -1)
        return Response.unpack(byte_arr)

    def send(self, operation: Operation):
        self.skt.send(Operation.pack(operation))

    def close(self):
        self.skt.close()
