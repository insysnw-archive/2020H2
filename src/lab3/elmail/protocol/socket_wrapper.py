import socket
import struct
import json

from src.lab3.elmail.protocol.message import Message


class SocketWrapper:
    def __init__(self, skt: socket, is_server=False):
        self.skt: socket = skt
        self.is_server = is_server

    def accept(self):
        client_socket, client_address = self.skt.accept()
        return SocketWrapper(client_socket), client_address

    def recv(self):
        try:
            byte_arr = self.skt.recv(struct.calcsize("!ii"))
            msg_type, content_len = struct.unpack("!ii", byte_arr)
            byte_arr = self.skt.recv(content_len)
            content = struct.unpack(f"! {content_len}s", byte_arr)[0].decode("utf-8").strip()
            content_json = json.loads(content)
        except ConnectionResetError:
            return Message(-1, None, None)
        except socket.timeout:
            return Message(-1, None, None)
        return Message(msg_type, content_len, content_json)

    def send(self, message):
        self.skt.send(message.pack())

    def close(self):
        self.skt.close()
