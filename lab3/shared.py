import io
import os
import types
import queue
import socket
import asyncio
import logging
import threading

class MessageFormatError(RuntimeError):
    pass

class Decoder:
    def __init__(self, reader):
        self.reader = reader
    
    def read_bytes(self, count):
        return self.reader.read(count)

    def _read_int(self, count, signed=False):
        chunk = self.read_bytes(count)
        return int.from_bytes(chunk, byteorder='big', signed=signed)
    
    def read_uint8(self):
        return self._read_int(1)

    def read_uint16(self):
        return self._read_int(2)
    
    def read_uint32(self):
        return self._read_int(4)
    
    def read_uint64(self):
        return self._read_int(8)
    
    def read_int8(self):
        return self._read_int(1, True)

    def read_int16(self):
        return self._read_int(2, True)
    
    def read_int32(self):
        return self._read_int(4, True)
    
    def read_int64(self):
        return self._read_int(8, True)
    
    def read_varint(self):
        result = 0
        for i in range(10):
            part = self.read_uint8()
            result |= (part & 0x7F) << 7 * i
            if not part & 0x80:
                return result
        raise MessageFormatError("varint is too big")
    
    def read_varint_big(self):
        result = 0
        i = 0
        while True:
            part = self.read_uint8()
            result |= (part & 0x7F) << 7 * i
            if not part & 0x80:
                return result
            i += 1

    def read_string(self):
        size = self.read_varint()
        return self.read_bytes(size).decode('utf-8')

class Encoder:
    def __init__(self, writer):
        self.writer = writer
    
    def write_bytes(self, value):
        self.writer.write(value)

    def _write_int(self, value, count, signed=False):
        chunk = value.to_bytes(count, byteorder='big', signed=signed)
        self.write_bytes(chunk)
    
    def write_uint8(self, value):
        self._write_int(value, 1)

    def write_uint16(self, value):
        self._write_int(value, 2)
    
    def write_uint32(self, value):
        self._write_int(value, 4)
    
    def write_uint64(self, value):
        self._write_int(value, 8)
    
    def write_int8(self, value):
        self._write_int(value, 1, True)

    def write_int16(self, value):
        self._write_int(value, 2, True)
    
    def write_int32(self, value):
        self._write_int(value, 4, True)
    
    def write_int64(self, value):
        self._write_int(value, 8, True)
    
    def write_varint(self, value):
        remaining = value
        for _ in range(5):
            if remaining & ~0x7F == 0:
                self.write_uint8(remaining)
                return
            self.write_uint8(remaining & 0x7F | 0x80)
            remaining >>= 7
        raise ValueError(f"the value {value} is too big to send in a varint")
    
    def write_varint_big(self, value):
        remaining = value
        while True:
            if remaining & ~0x7F == 0:
                self.write_uint8(remaining)
                return
            self.write_uint8(remaining & 0x7F | 0x80)
            remaining >>= 7

    def write_string(self, value: str):
        chunk = value.encode('utf-8')
        self.write_varint(len(chunk))
        self.write_bytes(chunk)

packet_types = []
packet_types_by_id = None

class PacketMetaclass(type):
    def __init__(self, name, bases, dct):
        super().__init__(name, bases, dct)
        if self.__name__ != 'Packet':
            packet_types.append(self)

class Packet(metaclass=PacketMetaclass):

    @property
    def id(self):
        return self.__class__.pid

    def decode(self, decoder: Decoder):
        pass

    def encode(self, encoder: Encoder):
        pass

async def read_varint_async(reader):
    result = 0
    for i in range(5):
        part = (await reader.readexactly(1))[0]
        result |= (part & 0x7F) << 7 * i
        if not part & 0x80:
            return result
    raise MessageFormatError("varint is too big")

async def recv_packet_async(reader) -> Packet:
    global packet_types_by_id
    size = await read_varint_async(reader)
    chunk = await reader.readexactly(size)
    decoder = Decoder(io.BytesIO(chunk))
    pid = decoder.read_uint8()
    if packet_types_by_id == None:
        packet_types_by_id = {packet_type.pid:packet_type for packet_type in packet_types}
    packet_type = packet_types_by_id[pid]
    packet = packet_type()
    packet.decode(decoder)
    return packet

async def send_packet_async(writer, packet: Packet):
    encoder = Encoder(io.BytesIO())
    encoder.write_uint8(packet.id)
    packet.encode(encoder)
    chunk = encoder.writer.getvalue()
    encoder.writer = io.BytesIO()
    encoder.write_varint(len(chunk))
    writer.write(encoder.writer.getvalue())
    writer.write(chunk)
    await writer.drain()

class FailurePacket(Packet):
    pid = 0

    def __init__(self, message=""):
        self.message = message

    def decode(self, decoder: Decoder):
        self.message = decoder.read_string()

    def encode(self, encoder: Encoder):
        encoder.write_string(self.message)

class ClientError(Exception):
    pass

class Respondent:
    def __init__(self, reader, writer):
        self.reader = reader
        self.writer = writer
        self.code_blocks = {}
        self.default_action = lambda p : FailurePacket(f"packet '{p.__class__.__name__}' not expected")

    def reply(self, packet_type):
        def prepare_code_block(function):
            self.code_blocks[packet_type.pid] = function
            return function
        return prepare_code_block

    def default(self, function):
        self.default_action = function
        return function

    async def run(self):
        while True:
            request = await recv_packet_async(self.reader)
            code_block = self.code_blocks.get(request.id, None)
            if code_block == None:
                code_block = self.default_action
            try:
                response = code_block(request)
                if isinstance(response, types.CoroutineType):
                    response = await response
                if isinstance(response, Packet):
                    await send_packet_async(self.writer, response)
            except ClientError as e:
                logging.exception('protocol logic error')
                if hasattr(e, 'message'):
                    message = e.message
                else:
                    message = str(e)
                await send_packet_async(self.writer, FailurePacket(message))
            except asyncio.IncompleteReadError:
                break

class Correspondent:
    def __init__(self, reader, writer):
        self.reader = reader
        self.writer = writer

    async def request(self, packet):
        await send_packet_async(self.writer, packet)
        response = await recv_packet_async(self.reader)
        if isinstance(response, FailurePacket):
            raise ClientError(response.message)
        return response

class Client:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.incomming = queue.LifoQueue()
        self.outcomming = queue.LifoQueue()
        self.on_error = lambda x: None

    def start(self):
        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.client_socket.connect((self.host, self.port))

        async def client_entrypoint():
            reader, writer = await asyncio.open_connection(sock=self.client_socket)
            self.correspondent = Correspondent(reader, writer)
            while True:
                packet = self.incomming.get()
                try:
                    self.outcomming.put(await self.correspondent.request(packet))
                except Exception as ex:
                    self.outcomming.put(ex)

        def client_thread():
            try:
                asyncio.run(client_entrypoint())
            except Exception as e:
                self.client_socket.close()
                on_error = self.on_error
                on_error(e)

        self.thr = threading.Thread(target=client_thread)
        self.thr.start()

    def request(self, packet: Packet) -> Packet:
        self.incomming.put(packet)
        result = self.outcomming.get()
        if isinstance(result, Exception):
            raise result
        else:
            return result
