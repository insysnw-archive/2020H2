import json
import struct


class Message:
    def __init__(self, msg_type, msg_content):
        self.msg_type = msg_type
        self.msg_content = msg_content
        self.msg_len = len(json.dumps(self.msg_content).encode())

    def pack(self):
        return struct.pack(f"!i i {self.msg_len}s", self.msg_type, self.msg_len, json.dumps(self.msg_content).encode())