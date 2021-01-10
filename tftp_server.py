import argparse
import sys
import struct
import os
from threading import Thread, Event
from socket import socket, AF_INET, SOCK_DGRAM

RRQ_MODE = 1
WRQ_MODE = 2
DATA_MODE = 3
ACK_MODE = 4
ERROR_MODE = 5

MAX_BYTES = 512
MAX_BUFFER_WRQ = 508

# Error codes
ERR_NOT_DEFINED = 0
ERR_FILE_NOT_FOUND = 1
ERR_ACCESS_VIOLATION = 2
ERR_DISK_FULL = 3
ERR_ILLEGAL_TFTP_OP = 4
ERR_UNKNOWN_TID = 5
ERR_FILE_ALREADY_EXISTS = 6
ERR_NO_SUCH_USER = 7


class FileContext:
    # context that is stored for each file transmission (RRQ and WRQ)
    def __init__(self, filename, block_num, timer, is_done=None):
        self.filename = filename
        self.block_num = block_num
        self.timer = timer
        self.is_done = is_done


class Server:
    def __init__(self, socket, timeout):
        self.sock = socket
        self.timeout = timeout

        # used to monitor read/write data transmission status
        self.reads = {}
        self.writes = {}

    def handle_RRQ(self, address, filename, block_num):
        # processes read transfer requests
        with open(filename, 'rb') as f:
            f.seek((block_num - 1) * MAX_BYTES, os.SEEK_SET)
            buf = f.read(MAX_BYTES)

        # can determine if the last ACK if num bytes less than the max
        is_done = len(buf) < MAX_BYTES
        packet = struct.pack('>hh%ds' % len(buf), DATA_MODE, block_num, buf)
        timer = Event()
        self.reads[address] = FileContext(filename, block_num, timer, is_done)

        print('Sent DATA', block_num)
        self.sock.sendto(packet, address)
        while not timer.wait(self.timeout):
            print('Retry DATA', block_num)
            self.sock.sendto(packet, address)

    def handle_WRQ(self, address, filename, block_num, buf=None):
        is_done = False
        if block_num > 0:
            with open(filename, 'wb') as f:
                f.seek((block_num - 1) * MAX_BYTES, os.SEEK_SET)
                f.write(buf)
            # can determine if the last DATA if num bytes less than the max
            is_done = len(buf) < MAX_BUFFER_WRQ

        packet = struct.pack('>hh', ACK_MODE, block_num)
        timer = Event()
        self.writes[address] = FileContext(filename, block_num, timer)

        print('Sent ACK', block_num)
        self.sock.sendto(packet, address)
        if not is_done:
            while not timer.wait(self.timeout):
                print('Retry ACK ', block_num)
                self.sock.sendto(packet, address)

    def handle_ACK(self, address, block_num):
        if address not in self.reads:
            self.handle_ERROR(address, ERR_UNKNOWN_TID, 'Unknown transfer ID')
            sys.exit()
        fc = self.reads[address]

        # ignore out-of-order or duplicate ACKS
        if fc.block_num == block_num:
            fc.timer.set()
            print('Received ACK', block_num)
            if not fc.is_done:
                self.handle_RRQ(address, fc.filename, fc.block_num + 1)

    def handle_DATA(self, address, block_num, buf):
        if address not in self.writes:
            self.handle_ERROR(address, ERR_UNKNOWN_TID, 'Unknown transfer ID')
            sys.exit()

        fc = self.writes[address]

        if fc.block_num + 1 == block_num:
            fc.timer.set()
            print('Received DATA ', block_num)
            self.handle_WRQ(address, fc.filename, fc.block_num + 1, buf)


    def handle_ERROR(self, address, code, msg):
        packet = struct.pack('>hh%dsb' % len((msg + '\0').encode()), ERROR_MODE, code, (msg + '\0').encode(), 0)
        print('Sent ERROR')
        sock.sendto(packet, address)

    def parse_packet(self, data, address):
        mode, rest = struct.unpack('>h', data[:2])[0], data[2:]

        if mode == RRQ_MODE:
            filename, mode = rest.decode().split('\0')[:2]

            if not os.path.exists(filename):
                self.handle_ERROR(address, ERR_FILE_NOT_FOUND, 'File does not exist')
                sys.exit()

            # in case of multiple RRQs in succession, terminate any previous transfer's timeouts
            if address in self.reads:
                self.reads[address].timer.set()
            print('Received RRQ')

            block_num = 1
            self.handle_RRQ(address, filename, block_num)

        elif mode == WRQ_MODE:
            filename, mode = rest.decode().split('\0')[:2]

            # in case of multiple WRQs in succession, terminate any previous transfer's timeouts
            if address in self.writes:
                self.writes[address].timer.set()
            print('Received WRQ')

            block_num = 0
            self.handle_WRQ(address, filename, block_num)

        elif mode == ACK_MODE:
            bnum = struct.unpack('>h', rest)[0]
            self.handle_ACK(address, bnum)

        elif mode == DATA_MODE:
            bnum, data = struct.unpack('>h', rest[:2])[0], rest[2:]
            self.handle_DATA(address, bnum, data)
        else:
            self.handle_ERROR(address, ERR_NOT_DEFINED, 'Invalid packet type')

    def run_server(self):
        while True:
            data, address = self.sock.recvfrom(MAX_BYTES)
            #print('Client: ', address)

            if data:
                t = Thread(target=self.parse_packet, args=(data, address))
                t.start()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-ip', default='0.0.0.0', type=str)
    parser.add_argument('-p', type=int)
    parser.add_argument('-t', type=int)

    arguments = parser.parse_args(sys.argv[1:])

    if arguments.p is None or arguments.t is None:
        print('Usage: python3 tftp_server.py -ip <ip> -p <port> -t <timeout> OR -p <port> -t <timeout>')
    else:
        port = int(arguments.p)
        timeout = int(arguments.t)
        ip = arguments.ip
        if ip is None:
            ip = ''

        sock = socket(AF_INET, SOCK_DGRAM)
        sock.bind((ip, port))
        print(f"___Server is ready!___")

        to = timeout / 1000
        server = Server(sock, to)
        server.run_server()
