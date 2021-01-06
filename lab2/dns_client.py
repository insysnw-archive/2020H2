#!/usr/bin/env python3

import argparse

parser = argparse.ArgumentParser(description='DNS client.')
parser.add_argument('--host', default='1.1.1.1', help='DNS server address')
parser.add_argument('--port', type=int, default=53, help='DNS server port number')
parser.add_argument('--type', default='A', help='record type')
parser.add_argument('domain', help='domain name for request')
args = parser.parse_args()

import ipaddress
import random
import socket
import json
import sys
import io

rtypes = []

class RecordMetaclass(type):
    def __init__(self, name, bases, dct):
        super().__init__(name, bases, dct)
        #if not isinstance(self.id, int):
        #    raise RuntimeError("id field is not an int!")
        if self.__name__ != 'Record':
            rtypes.append(self)

class Record(metaclass=RecordMetaclass):
    def decode(self, decoder):
        raise NotImplementedError()

class DNSFormatError(RuntimeError):
    pass

class Decoder:
    def __init__(self, data, start, end):
        self._data = data
        self._start = start
        self._end = end

    def __len__(self):
        return self._end - self._start
    
    def read_int8(self):
        pos = self._start
        if pos >= self._end:
            raise DNSFormatError("end of packet chunk")
        self._start = pos + 1
        return self._data[pos]

    def read_int8_global(self, pos):
        return self._data[pos]
    
    def read_chunk(self, count):
        start = self._start
        pos = start + count
        if pos > self._end:
            raise DNSFormatError("end of packet chunk")
        self._start = pos
        return self._data[start:pos]

    def read_chunk_global(self, pos, count):
        return self._data[pos:(pos+count)]

    def _read_int(self, count):
        chunk = self.read_chunk(count)
        return int.from_bytes(chunk, byteorder='big', signed=False)

    def read_int16(self):
        return self._read_int(2)
    
    def read_int32(self):
        return self._read_int(4)

    def read_name_global(self, pos):
        size = self.read_int8_global(pos)
        if size == 0:
            return ""
        elif (size >> 6) == 3:
            pos = ((size & 63) << 8) | self.read_int8_global(pos + 1)
            return self.read_name_global(pos)
        else:
            label = self.read_chunk_global(pos + 1, size).decode('ascii')
            return f"{label}.{self.read_name_global(pos + 1 + size)}"

    def read_name(self):
        size = self.read_int8()
        if size == 0:
            return ""
        elif (size >> 6) == 3:
            pos = ((size & 63) << 8) | self.read_int8()
            return self.read_name_global(pos)
        else:
            label = self.read_chunk(size).decode('ascii')
            return f"{label}.{self.read_name()}"

    def read_record_data(self, size):
        start = self._start
        pos = start + size
        if pos > self._end:
            raise DNSFormatError("end of packet chunk")
        self._start = pos
        return Decoder(self._data, start, pos)

class Encoder:
    def __init__(self, writer):
        self._writer = writer

    def write_chunk(self, value):
        self._writer.write(value)

    def _write_int(self, value, count):
        value_bytes = value.to_bytes(count, byteorder='big', signed=False)
        self.write_chunk(value_bytes)

    def write_int8(self, value):
        self._write_int(value, 1)

    def write_int16(self, value):
        self._write_int(value, 2)

    def write_int32(self, value):
        self._write_int(value, 4)

    def write_name(self, value):
        labels = value.split('.')
        for label in labels:
            label_bytes = label.encode('ascii')
            self.write_int8(len(label_bytes))
            self.write_chunk(label_bytes)
        self.write_int8(0)

class Unknown(Record):
    id = 0

    def decode(self, decoder):
        pass

class A(Record):
    id = 1

    def decode(self, decoder):
        self.address = ipaddress.IPv4Address(decoder.read_int32())

class NS(Record):
    id = 2

    def decode(self, decoder):
        self.ns_domain = decoder.read_name()

class MX(Record):
    id = 15

    def decode(self, decoder):
        self.preference = decoder.read_int16()
        self.exchange = decoder.read_name()

records = {record_type.id:record_type for record_type in rtypes}
records_by_name = {record_type.__name__:record_type for record_type in rtypes}

class Question:
    def __init__(self, rtype = 1, name = ""):
        self.type_id = rtype
        self.name = name

    def encode(self, encoder):
        encoder.write_name(self.name)
        encoder.write_int16(self.type_id)
        encoder.write_int16(1)      # qclass = IN
        pass

    def decode(self, decoder):
        self.name = decoder.read_name()
        self.type_id = decoder.read_int16()
        type_name = records.get(self.type_id)
        if type_name != None:
            self.type = type_name.__name__
        decoder.read_int16() # qclass

def read_question(decoder):
    question = Question()
    question.decode(decoder)
    return question

def read_record(decoder):
    name = decoder.read_name()
    rtype = decoder.read_int16()
    decoder.read_int16() # read class
    ttl = decoder.read_int32()
    size = decoder.read_int16()
    record_decoder = decoder.read_record_data(size)
    record_type = records.get(rtype, Unknown)
    record = record_type()
    record.ttl = ttl
    record.type_id = rtype
    record.type = record_type.__name__
    record.name = name
    record.decode(record_decoder)
    return record

opcodes = {0:'query', 1:'iquery', 2:'status'}
rcodes  = {0:'no error', 1:'format error', 2:'server failure', 3:'name error', 4:'not implemented', 5:'refused'}

class Packet:
    def decode(self, decoder):
        self.id = decoder.read_int16()
        octet = decoder.read_int8()
        self.is_response = bool(octet >> 7)
        opcode = (octet >> 3) & 15
        self.operation_code = opcodes.get(opcode, opcode)
        self.authority_answer = bool((octet >> 2) & 1)
        self.trancated = bool((octet >> 1) & 1)
        self.recursion_desired = bool(octet & 1)
        octet = decoder.read_int8()
        self.recursion_available = bool(octet >> 7)
        rcode = octet & 15
        self.response_code = rcodes.get(rcode, rcode)
        qdcount = decoder.read_int16()
        ancount = decoder.read_int16()
        nscount = decoder.read_int16()
        arcount = decoder.read_int16()

        self.questions = [read_question(decoder) for i in range(qdcount)]
        self.answers = [read_record(decoder) for i in range(ancount)]
        self.name_servers = [read_record(decoder) for i in range(nscount)]
        self.additional_answers = [read_record(decoder) for i in range(arcount)]

def prepare_request(encoder, question):
    # head
    id = random.randint(0, 65536)
    encoder.write_int16(id)     # id = random
    encoder.write_int8(1)       # qr = 0, opcode = 0, aa = 0, tc = 0, rd = 1
    encoder.write_int8(0)       # ra = 0, z = 0, rcode = 0
    encoder.write_int16(1)      # qdcount = 1
    encoder.write_int16(0)      # ancount = 0
    encoder.write_int16(0)      # nscount = 0
    encoder.write_int16(0)      # arcount = 0
    # questions
    question.encode(encoder)

def serializer(obj):
    if isinstance(obj, ipaddress.IPv4Address):
        return str(obj)
    return obj.__dict__

client_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

writer = io.BytesIO()
encoder = Encoder(writer)

try:
    question = Question(int(args.type), args.domain)
except ValueError:
    record = records_by_name[args.type]
    if record == None:
        sys.stderr.write(f"unknown record type {args.type}\n")
        exit(1)
    question = Question(record.id, args.domain)

prepare_request(encoder, question)
writer.seek(0)
encoded_packet = writer.read()
client_socket.sendto(encoded_packet, (args.host, args.port))
encoded_packet, address = client_socket.recvfrom(10000)
decoder = Decoder(encoded_packet, 0, len(encoded_packet))
packet = Packet()
packet.decode(decoder)
print(json.dumps(packet, default=serializer))
