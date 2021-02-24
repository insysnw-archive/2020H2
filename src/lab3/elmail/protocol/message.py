import json
import struct


class Message:
    def __init__(self, msg_type, msg_len, msg_content):
        self.msg_type = msg_type
        self.msg_len = msg_len
        self.msg_content = msg_content

    def pack(self):
        return struct.pack(f"!i i {self.msg_len}s", self.msg_type, self.msg_len, json.dumps(self.msg_content).encode())
