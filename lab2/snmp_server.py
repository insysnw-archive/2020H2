#!/usr/bin/env python3

import argparse

parser = argparse.ArgumentParser(description='Simple SNMP agent.')
parser.add_argument('--host', default='0.0.0.0', help='address to bind to')
parser.add_argument('--port', type=int, default = 161, help='server port number')
parser.add_argument('--repository', default='agent.toml', help='configuration file path')
parser.add_argument('--engine', default='ssnmp', help='engine id')
args = parser.parse_args()

import io
import sys
import toml
import socket
import logging
import datetime
import ipaddress

BUFFER_SIZE = 4000
APPLICATION = 0x40
engine_time = 0

data_types = []
data_types_by_id = None
data_types_by_name = None

logging.basicConfig(level=logging.INFO)

def read_varint(reader):
    result = 0
    for i in range(5):
        octet = reader.read(1)
        if len(octet) != 1:
            return None
        part = octet[0]
        result |= (part & 0x7F) << 7 * i
        if not part & 0x80:
            return result
    raise IOError("Server sent a varint that was too big!")

def write_varint(writer, value):
    remaining = value
    for _ in range(5):
        if remaining & ~0x7F == 0:
            writer.write(remaining.to_bytes(1, byteorder='big', signed=False))
            return
        writer.write((remaining & 0x7F | 0x80).to_bytes(1, byteorder='big', signed=False))
        remaining >>= 7
    raise ValueError("The value %d is too big to send in a varint" % value)

def decode_next(reader):
    octets = reader.read(1)
    if len(octets) != 1:
        return None
    tid = octets[0]
    length = read_varint(reader)
    chunk = reader.read(length)
    type_class = data_types_by_id.get(tid, None)
    if type_class == None:
        logging.info('unknown type id: %s', tid)
    next_value = type_class()
    next_value.decode(chunk)
    return next_value

def encode_next(writer, next_value):
    writer.write(next_value.__class__.type_id.to_bytes(1, byteorder='big', signed=False))
    chunk = next_value.encode()
    write_varint(writer, len(chunk))
    writer.write(chunk)

def oid2str(value):
    return '.'.join([str(x) for x in value])

def str2oid(value):
    return tuple([int(x) for x in value.split('.')])

class DataTypeMetaclass(type):
    def __init__(self, name, bases, dct):
        super().__init__(name, bases, dct)
        #if not isinstance(self.id, int):
        #    raise RuntimeError("id field is not an int!")
        if self.__name__ != 'DataType':
            data_types.append(self)
    
    def __call__(cls, *args, **kwargs):
        self = super().__call__(*args, **kwargs)
        self.mutable = False
        return self

class DataType(metaclass=DataTypeMetaclass):
    aliases = []

    def __init__(self):
        self.value = None
        self.mutable = False

    def decode(self, data):
        raise NotImplementedError()

    def encode(self):
        raise NotImplementedError()

    def value_to_string(self):
        return str(self.value)

    def __str__(self):
        value_str = self.value_to_string()
        name = self.__class__.__name__
        if self.mutable:
            return f"{name}<mutable>({value_str})"
        else:
            return f"{name}({value_str})"
    
    def __bytes__(self):
        writer = io.BytesIO()
        encode_next(writer, self)
        return writer.getvalue()


class INTEGER(DataType):
    type_id = 0x02

    aliases = ['int']

    def __init__(self, value=0, size=4, signed=True):
        if isinstance(value, str):
            self.value = int(value)
        else:
            self.value = value
        self.size = size
        self.signed = signed

    def decode(self, data):
        self.size = len(data)
        self.value = int.from_bytes(data, byteorder='big', signed=self.signed)

    def encode(self):
        return self.value.to_bytes(length=self.size, byteorder='big', signed=self.signed)

class OCTET_STRING(DataType):
    type_id = 0x04

    aliases = ['octet string', 'string', 'str']

    def __init__(self, value=b''):
        if isinstance(value, str):
            self.value = bytes(value, 'utf-8')
        else:
            self.value = value

    def decode(self, data):
        self.value = data
    
    def encode(self):
        return self.value

class NULL(DataType):
    type_id = 0x05
    
    def decode(self, data):
        pass

    def encode(self):
        return b''

class OBJECT_IDENTIFIER(DataType):
    type_id = 0x06

    aliases = ['object identifier', 'oid']

    def __init__(self, value=None):
        if value == None:
            self.value = (1, 3)
        elif isinstance(value, str):
            self.value = str2oid(value)
        else:
            self.value = value

    def decode(self, data):
        reader = io.BytesIO(data)
        octet = reader.read(1)[0]
        oid = [octet // 40, octet % 40]
        while True:
            next = read_varint(reader)
            if next == None:
                break
            oid.append(next)
        self.value = tuple(oid)
    
    def encode(self):
        writer = io.BytesIO()
        octet = self.value[0]*40 + self.value[1]
        writer.write(octet.to_bytes(1, byteorder='big', signed=False))
        for x in self.value[2:]:
            write_varint(writer, x)
        return writer.getvalue()

    def value_to_string(self):
        return oid2str(self.value)

class IPADDRESS(DataType):
    type_id = APPLICATION + 0

    aliases = ['ip']

    def __init__(self, value=ipaddress.ip_address('127.0.0.1')):
        if isinstance(value, str):
            self.value = ipaddress.ip_address(value)
        else:
            self.value = value

    def decode(self, data):
        length = len(data)
        if length == (ipaddress.IPV4LENGTH // 8):
            self.value = ipaddress.IPv4Address(data)
        elif length == (ipaddress.IPV6LENGTH // 8):
            self.value = ipaddress.IPv6Address(data)
        else:
            raise ValueError(f"unknown ipaddress size: {length}")

    def encode(self):
        return self.value.packed

class COUNTER32(INTEGER):
    type_id = APPLICATION + 1

    aliases = ['cnt', 'cnt32']

    def __init__(self, value=None):
        super().__init__(value, size=4, signed=False)

class UNSIGNED32(COUNTER32):
    type_id = APPLICATION + 2

    aliases = ['uint', 'gauge32']

class TIMETICKS(DataType):
    type_id = APPLICATION + 3

    aliases = ['datetime']

    epoch = datetime.datetime.utcfromtimestamp(0)

    def __init__(self, value=None):
        if isinstance(value, str):
            raise ValueError('use TOML datetime type instead')
        elif value == None:
            self.value = datetime.datetime.now()
        else:
            self.value = value
    
    def decode(self, data):
        inner = INTEGER(size=8, signed=False)
        inner.decode(data)
        self.value = datetime.datetime.fromtimestamp(inner.value / 100.0)

    def encode(self):
        inner = INTEGER(int((self.value - TIMETICKS.epoch).total_seconds() * 100.0), size=8, signed=False)
        return inner.encode()

    def value_to_string(self):
        return self.value.strftime('%Y-%m-%dT%H:%M:%S.%f')

class OPAQUE(OCTET_STRING):
    type_id = APPLICATION + 4

class COUNTER64(INTEGER):
    type_id = APPLICATION + 1

    aliases = ['cnt64']

    def __init__(self, value=None):
        super().__init__(value, size=8, signed=False)

class SEQUENCE(DataType):
    type_id = 0x30

    aliases = ['seq']

    def __init__(self, value=None):
        if value == None:
            self.value = []
        elif isinstance(value, str):
            raise ValueError('sequence should be represented as TOML array')
        else:
            self.value = value

    def decode(self, data):
        reader = io.BytesIO(data)
        while True:
            next_value = decode_next(reader)
            if next_value == None:
                break
            self.value.append(next_value)

    def encode(self):
        writer = io.BytesIO()
        for next_value in self.value:
            encode_next(writer, next_value)
        return writer.getvalue()

    def value_to_string(self):
        values = [str(x) for x in self.value]
        return '[' + ', '.join(values) + ']'

    def __getitem__(self, key):
        if isinstance(key, int):
            return self.value[key]
        else:
            for item in self.value:
                if isinstance(item, key):
                    return item
            raise TypeError(f"type '{key.__name__}' not found in list")
    
    def append(self, value):
        self.value.append(value)

class NOSUCHOBJECT(NULL):
    type_id = 0x80

class NOSUCHINSTANCE(NULL):
    type_id = 0x81

class ENDOFMIBVIEW(NULL):
    type_id = 0x82

PDU = 0xA0

class GET_REQUEST(SEQUENCE):
    type_id = PDU + 0

class GET_NEXT_REQUEST(SEQUENCE):
    type_id = PDU + 1

class RESPONSE(SEQUENCE):
    type_id = PDU + 2

class SET_REQUEST(SEQUENCE):
    type_id = PDU + 3

class GET_BULK_REQUEST(SEQUENCE):
    type_id = PDU + 5

class INFORM_REQUEST(SEQUENCE):
    type_id = PDU + 6

class TRAP(SEQUENCE):
    type_id = PDU + 7

class REPORT(SEQUENCE):
    type_id = PDU + 8

data_types_by_id = {data_type.type_id:data_type for data_type in data_types}
data_types_by_name = {name.lower():data_type for data_type in data_types for name in data_type.aliases + [data_type.__name__]}

repository = {}

config = toml.load(args.repository)

def int_or_none(x):
    try:
        return int(x)
    except ValueError:
        return None

def dict_merge(dct, merge_dct):
    for k in merge_dct:
        if (k in dct and isinstance(dct[k], dict)):
            dict_merge(dct[k], merge_dct[k])
        else:
            dct[k] = merge_dct[k]

def prepare_variable(node, type=OCTET_STRING, mutable=False):
    data = None
    if isinstance(node, str):
        data = type(node)
    elif isinstance(node, int):
        data = INTEGER(node)
    elif isinstance(node, datetime.datetime):
        data = TIMETICKS(node)
    elif isinstance(node, list):
        data = SEQUENCE([prepare_variable(x, type, mutable) for x in node])
    elif isinstance(node, dict):
        data = {}
        for key, value in node.items():
            branch = int_or_none(key)
            if branch == None:
                lkey = key.lower()
                to_merge = None
                if lkey == 'mutable':
                    to_merge = prepare_variable(value, type, True)
                else:
                    data_type = data_types_by_name[lkey]
                    to_merge = prepare_variable(value, data_type, mutable)
                if isinstance(to_merge, dict):
                    dict_merge(data, to_merge)
                else:
                    return to_merge
            else:
                data[branch] = prepare_variable(value, type, mutable)
        return data
    else:
        raise ValueError('unsupported toml type')
    data.mutable = mutable
    return data

repository = prepare_variable(config)

def stringify(node):
    if isinstance(node, dict):
        return {key:stringify(value) for key, value in node.items()}
    else:
        return str(node)

logging.info(f"repository: {stringify(repository)}")

def repo_find(oid):
    value = repository
    for branch in oid:
        value = value.get(branch, None)
        if value == None:
            return None
    return value

def dict_next(dictionary, key):
    it = iter(sorted(dictionary.keys()))
    for k in it:
        if k == key:
            return next(it)
    raise StopIteration()

def repo_go_down(oid):
    value = repo_find(oid)
    while isinstance(value, dict):
        branch = next(iter(sorted(value.keys())))
        oid.append(branch)
        value = value[branch]
    return value

def repo_go_up(oid):
    while len(oid) > 0:
        key = oid.pop()
        try:
            value = repo_find(oid)
            key = dict_next(value, key)
            oid.append(key)
            return value[key]
        except StopIteration:
            pass
    raise StopIteration()

def repo_find_next(roid):
    oid = list(roid)
    while True:
        try:
            before = len(oid)
            value = repo_go_down(oid)
            if before != len(oid):
                return tuple(oid), value
        except StopIteration:
            pass
        try:
            value = repo_go_up(oid)
            if not isinstance(value, dict):
                return tuple(oid), value
        except StopIteration:
            return roid, ENDOFMIBVIEW()

class SecurityModel:
    def decode(self, data):
        raise NotImplementedError()

    def encode(self):
        raise NotImplementedError()

    def __str__(self):
        raise NotImplementedError()

class UserBasedSecurityModel(SecurityModel):
    def __init__(self):
        self.authoritative_engine_id = bytes(args.engine, 'utf-8')
        self.authoritative_engine_boots = 1
        self.authoritative_engine_time = 0
        self.username = b'nobody'
        self.authentication_parameters = b''
        self.privacy_parameters = b''

    def decode(self, data):
        reader = io.BytesIO(data)
        sequence = decode_next(reader)
        self.authoritative_engine_id = sequence[0].value
        self.authoritative_engine_boots = sequence[1].value
        self.authoritative_engine_time = sequence[2].value
        self.username = sequence[3].value
        self.authentication_parameters = sequence[4].value
        self.privacy_parameters = sequence[5].value

    def encode(self):
        sequence = SEQUENCE([
            OCTET_STRING(self.authoritative_engine_id),
            INTEGER(self.authoritative_engine_boots, signed=False),
            INTEGER(self.authoritative_engine_time, signed=False),
            OCTET_STRING(self.username),
            OCTET_STRING(self.authentication_parameters),
            OCTET_STRING(self.privacy_parameters)
        ])
        return bytes(sequence)
    
    def __str__(self):
        return f"(authoritative_engine_id={self.authoritative_engine_id}, authoritative_engine_boots={self.authoritative_engine_boots}, authoritative_engine_time={self.authoritative_engine_time}, username={self.username}, authentication_parameters={self.authentication_parameters}, privacy_parameters={self.privacy_parameters})"

security_models = { 3:UserBasedSecurityModel }

class Packet:
    def __init__(self, sequence=None):
        if sequence == None:
            self.version = 3
            self.header = Packet.Header()
            self.security_parameters = UserBasedSecurityModel()
            self.data = Packet.ScopedPDU()
        else:
            self.version = sequence[0].value
            self.header = Packet.Header(sequence[1])
            security_model = security_models[self.header.security_model]
            security_parameters = security_model()
            security_parameters.decode(sequence[2].value)
            self.security_parameters = security_parameters
            self.data = Packet.ScopedPDU(sequence[3])

    def to_asn1(self):
        return SEQUENCE([INTEGER(self.version, 1), self.header.to_asn1(), OCTET_STRING(self.security_parameters.encode()), self.data.to_asn1()])

    def __str__(self):
        return f"(version={self.version}, header={self.header}, security_parameters={self.security_parameters}, data={self.data})"

    class Header:
        def __init__(self, sequence=None):
            if sequence == None:
                self.id = 0
                self.max_size = BUFFER_SIZE
                self.flags = Packet.Header.Flags()
                self.security_model = 3
            else:
                self.id = sequence[0].value
                self.max_size = sequence[1].value
                self.flags = Packet.Header.Flags(sequence[2].value)
                self.security_model = sequence[3].value

        def to_asn1(self):
            return SEQUENCE([
                INTEGER(self.id),
                INTEGER(self.max_size, size=4, signed=False),
                OCTET_STRING(bytes(self.flags)),
                INTEGER(self.security_model, size=1, signed=False)
            ])

        def __str__(self):
            return f"(id={self.id}, max_size={self.max_size}, flags={self.flags}, security_model={self.security_model})"

        class Flags:
            def __init__(self, octets=None):
                if octets == None:
                    self.auth = False
                    self.priv = False
                    self.reportable = False
                else:
                    octet = octets[0]
                    self.auth = bool(octet & 1)
                    self.priv = bool(octet & 2)
                    self.reportable = bool(octet & 4)

            def __bytes__(self):
                octet = 0
                if self.auth:
                    octet |= 1
                if self.priv:
                    octet |= 2
                if self.reportable:
                    octet |= 4
                return octet.to_bytes(1, byteorder='big', signed=False)

            def __str__(self):
                return f"(auth={self.auth}, priv={self.priv}, reportable={self.reportable})"

    class ScopedPDU:
        def __init__(self, sequence=None):
            if isinstance(sequence, OCTET_STRING):
                raise ValueError('encrypted PDU is not supported')
            elif sequence == None:
                self.engine_id = b''
                self.name = b''
                self.data = None
            else:
                self.engine_id = sequence[0].value
                self.name = sequence[1].value
                self.data = sequence[2]

        def to_asn1(self):
            return SEQUENCE([OCTET_STRING(self.engine_id), OCTET_STRING(self.name), self.data])

        def __str__(self):
            return f"(engine_id={self.engine_id}, name={self.name}, data={self.data})"

errors = [
    'no error',
    'too big',
    'no such name',
    'bad value',
    'read only',
    'gen err',
    'no access',
    'wrong type',
    'wrong length',
    'wrong encoding',
    'wrong value',
    'no creation',
    'inconsistent value',
    'resource unavailable',
    'commit failed',
    'undo failed',
    'authorization error',
    'not writable',
    'inconsistent name'
]

class Pdu:
    def __init__(self, sequence=None):
        if sequence == None:
            self.request_id = 0
            self.error_status = errors[0]
            self.error_index = 0
            self.variables = {}
            self.type = RESPONSE
        else:
            self.request_id = sequence[0].value
            self.error_status = errors[sequence[1].value]
            self.error_index = sequence[2].value
            self.variables = {x[0].value:x[1] for x in sequence[3].value}
            self.type = sequence.__class__

    def to_asn1(self):
        return self.type([
            INTEGER(self.request_id),
            INTEGER(errors.index(self.error_status), size=1, signed=False),
            INTEGER(self.error_index, signed=False),
            SEQUENCE([
                SEQUENCE([OBJECT_IDENTIFIER(key), value]) for key, value in self.variables.items()
            ])
        ])

    def __getitem__(self, key):
        if not isinstance(key, str):
            key = str2oid(key)
        return self.variables[key]

    def __setitem__(self, key, value):
        if isinstance(key, str):
            key = str2oid(key)
        self.variables[key] = value

class BulkPdu:
    def __init__(self, sequence=None):
        if sequence == None:
            self.request_id = 0
            self.non_repeaters = 0
            self.max_repetitions = 0
            self.variables = dict()
            self.type = GET_BULK_REQUEST
        else:
            self.request_id = sequence[0].value
            self.non_repeaters = sequence[1].value
            self.max_repetitions = sequence[2].value
            self.variables = {x[0].value:x[1] for x in sequence[3].value}
            self.type = sequence.__class__
    
    def to_asn1(self):
        return self.type([
            INTEGER(self.request_id),
            INTEGER(self.non_repeaters, signed=False),
            INTEGER(self.max_repetitions, signed=False),
            SEQUENCE([
                SEQUENCE([OBJECT_IDENTIFIER(key), value]) for key, value in self.variables.items()
            ])
        ])

    def __getitem__(self, key):
        return self.variables[key]

    def __setitem__(self, key, value):
        self.variables[key] = value

def handle_GET(data):
    in_pdu = Pdu(data)
    out_pdu = Pdu()
    out_pdu.request_id = in_pdu.request_id
    out_pdu.type = RESPONSE
    for key in in_pdu.variables:
        value = repo_find(key)
        if value == None:
            out_pdu[key] = NOSUCHOBJECT()
        elif isinstance(value, dict):
            out_pdu[key] = NULL
            out_pdu.error_status = 'gen err'
            out_pdu.error_index = 1 # abstraction level do not allow me to set right value properly
        else:
            out_pdu[key] = value
    return out_pdu.to_asn1()

def handle_GET_NEXT(data):
    in_pdu = Pdu(data)
    out_pdu = Pdu()
    out_pdu.request_id = in_pdu.request_id
    out_pdu.type = RESPONSE
    for key in in_pdu.variables:
        oid, value = repo_find_next(key)
        out_pdu[oid] = value
    return out_pdu.to_asn1()

def handle_SET(data):
    pdu = Pdu(data)
    pdu.type = RESPONSE
    index = 0
    for key in pdu.variables:
        value = repo_find(key)
        new_value = pdu.variables[key]
        if value == None:
            pdu.error_status = 'no creation'
            pdu.error_index = index
            break
        elif not value.mutable:
            pdu.error_status = 'not writable'
            pdu.error_index = index
            break
        elif value.__class__ != new_value.__class__:
            pdu.error_status = 'wrong type'
            pdu.error_index = index
            break
        index += 1
    if pdu.error_status == 'no error':
        for key in pdu.variables:
            value = repo_find(key)
            value.value = pdu.variables[key].value
    return pdu.to_asn1()

server_socket = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)

server_socket.bind((args.host, args.port))

logging.info("starting server on %s", server_socket.getsockname())

while True:
    try:
        message, address = server_socket.recvfrom(BUFFER_SIZE)
        sequence = decode_next(io.BytesIO(message))
        logging.debug("message from %s", address)
        logging.debug("incoming asn.1 object: %s", sequence)
        packet = Packet(sequence)
        logging.info("incoming packet: %s", packet)

        reportable = packet.header.flags.reportable and packet.data.engine_id == b''

        pdu = packet.data.data
        if reportable:
            pdu = Pdu()
            pdu.type = REPORT
            pdu = pdu.to_asn1()
        elif isinstance(pdu, GET_REQUEST):
            pdu = handle_GET(pdu)
        elif isinstance(pdu, GET_NEXT_REQUEST):
            pdu = handle_GET_NEXT(pdu)
        elif isinstance(pdu, SET_REQUEST):
            pdu = handle_SET(pdu)
        else:
            raise NotImplementedError(f"pdu handler is not implemented for {pdu.__type__.__name__}")

        packet.data.data = pdu
        packet.header.flags.reportable = False
        packet.data.engine_id = bytes(args.engine, 'utf-8')
        packet.security_parameters.authoritative_engine_id = args.engine
        packet.security_parameters.authoritative_engine_time = engine_time
        packet.security_parameters.authoritative_engine_boots = 2
        engine_time += 1

        logging.info("outcoming packet: %s", packet)

        sequence = packet.to_asn1()
        logging.debug("outcoming asn.1 object: %s", sequence)
        
        answer = bytes(sequence)

        server_socket.sendto(answer, address)
    except KeyboardInterrupt:
        server_socket.close()
        logging.info('server socket closed')
        exit(0)
    except Exception:
        logging.exception('error while processing request')
