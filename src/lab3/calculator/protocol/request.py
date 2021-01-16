import struct
import math

FORMAT = "!H H 3f"

max_operand_value = 3.4 * 10**38


class Operation:
    def __init__(self, id: int, type: int, oper1: float, oper2: float = 0.0, timeout: int = 0):
        self.id = id
        self.type = type
        if math.fabs(oper1) > max_operand_value:
            oper1 = max_operand_value if oper1 >= 0 else -max_operand_value
            print("Внимание! Первый операнд имеет размерность больше чем 4 байта! Это приведет к искажению результата!")
        if math.fabs(oper2) > max_operand_value:
            oper2 = max_operand_value if oper2 >= 0 else -max_operand_value
            print("Внимание! Второй операнд имеет размерность больше чем 4 байта! Это приведет к искажению результата!")
        if math.fabs(timeout) > max_operand_value:
            timeout = max_operand_value if timeout >= 0 else -max_operand_value
            print("Внимание! Таймаут имеет размерность больше чем 4 байта! Это приведет к искажению результата!")
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
