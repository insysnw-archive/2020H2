import struct

FORMAT = "!H H 3f"


class Operation:
    def __init__(self, id: int, type: int, oper1: float, oper2: float=0.0, timeout: int = 0):
        self.id = id
        self.type = type
        self.operand1 = oper1
        self.operand2 = oper2
        self.timeout = timeout

    @staticmethod
    def unpack(byte_arr: bytes):
        if not len(byte_arr):
            return None
        res = struct.unpack(FORMAT, byte_arr)
        return Operation(res[0], res[1], res[2], res[3], res[4])

    @staticmethod
    def pack(operation) -> bytes:
        return struct.pack(
            FORMAT,
            operation.id,
            operation.type,
            operation.operand1,
            operation.operand2,
            operation.timeout,
        )

    def size(self) -> int:
        return struct.calcsize(FORMAT)


# op = Operation(1, 1, 15)
# packed = Operation.pack(op)
# unpacked = Operation.unpack(packed)
# print(unpacked.__dict__)
