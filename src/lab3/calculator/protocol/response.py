import struct

FORMAT = "!2H f 40s"


class Response:
    def __init__(self, operation_id: int, code: int, result: float, msg: str = "", need_format=True):
        self.operation_id = operation_id
        self.code = code
        self.result = result
        msg_len = len(msg)
        if need_format:
            if msg_len < 40:
                msg += " " * (40 - msg_len)
            if msg_len > 40:
                msg = msg[0:40]
        self.msg = msg

    @staticmethod
    def pack(response) -> bytes:
        return struct.pack(FORMAT, response.operation_id, response.code, response.result, response.msg.encode())

    @staticmethod
    def unpack(byte_arr: bytes):
        if not len(byte_arr):
            return None
        res = struct.unpack(FORMAT, byte_arr)
        return Response(res[0], res[1], res[2], res[3].decode("utf-8").strip(), False)

    def size(self) -> int:
        return struct.calcsize(FORMAT)
