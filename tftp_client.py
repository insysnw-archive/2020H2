import socket
import argparse
import random
import shlex


# tftp opcodes
RRQ = 1  # read request
WRQ = 2  # write request
DATA = 3  # data
ACK = 4  # acknowledgment
ERROR = 5  # error

mode = 'netascii'  # mode for tftp transfer, netascii by default

error_codes = {
    0: 'Not defined, see error message (if any)',
    1: 'File not found',
    2: 'Access violation',
    3: 'Disk full or allocation exceeded',
    4: 'Illegal TFTP operation',
    5: 'Unknown transfer ID',
    6: 'File already exists',
    7: 'No such user'
}

# command line arguments parser
parser = argparse.ArgumentParser(description='TFTP client')
parser.add_argument("--ip", default='127.0.0.1', type=str, help='server address')
parser.add_argument("-d", default=0, type=bool, help='show details: 1 to show all incoming and outcoming messages')
args = parser.parse_args()
server_ip = args.ip
print('server_ip', server_ip)
show_details = args.d

# from RFC: the TID's chosen for a connection should be randomly chosen. 
# These TID's are handed to the supporting UDP (or other datagram protocol) as the source and destination ports.
tid = random.randint(1025, 65535)

# creating and binding UDP-socket
client_address = socket.gethostbyname(socket.gethostname())
print('TFTP client starting at', client_address,':', tid)
client_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
client_socket.bind((client_address, tid))

server_tid = 69  # initial TID for request is 69 by RFC
server_address = (server_ip, server_tid)
print('Server address:', server_address)

while True:
    server_tid = 69  # initial TID for request is 69 by RFC
    cmd, *args = shlex.split(input('> '))

    if cmd=='exit':
        break

    elif cmd=='help':
        print('read <file> - send read request to access file with specified name')
        print('write <file> - send write request to write into file with specified name')
        print('ascii - go into netascii transfer mode, default')
        print('binary - go into octet transfer mode')

    elif cmd=='ascii':  # netascii transfer mode, default
        mode = 'netascii'

    elif cmd=='binary':  # octet transfer mode
        mode = 'octet'

    elif cmd=='read':
        msg = bytearray([0, RRQ]) + bytearray(shlex.join(args).encode('ascii')) + bytes([0]) + bytearray(mode.encode('ascii')) + bytes([0])
        client_socket.sendto(msg, server_address)

        while True:
            msg, addr = client_socket.recvfrom(516)  # 4 bytes for header, 512 max data size according to RFC

            if msg[0] != 0 and msg[1] not in range(1,6):  # check opcode in TFTP packet
                print('Unknown TFTP opcode')
                continue

            if addr[1] != server_tid:  # check server's TID for case when first packet from server got duplicated somewhere in the network
                if server_tid == 69:
                    server_tid = addr[1]
                else:
                    continue

            print('TFTP opcode:', msg[1])

            if msg[1] == 5:  # error received
                print('Error code:', msg[3], error_codes[msg[3]])
                print('Error message:', msg[4:])
                if msg[3] == 5:  # unknown TID errors don't terminate the connection
                    continue
                else:
                    break

            if msg[1] == 3:  # data received
                print('Block number received:', msg[2] + msg[3])
                reply = bytearray([0, 4, msg[2], msg[3]])  # forming acknowledgement

                if show_details:
                    print('Data:\n', msg[4:516])
                    print('Reply:\n', reply)

                client_socket.sendto(reply, addr)

                if len(msg) < 516:  # if data field is less than 512 - that's last packet (512 + 4 header bytes = 516)
                    break

    elif cmd=='write':
        msg = bytearray([0, WRQ]) + bytearray(shlex.join(args).encode('ascii')) + bytes([0]) + bytearray(mode.encode('ascii'))+ bytes([0])
        client_socket.sendto(msg, server_address)

        print('Write data to file:')
        data = str(input())
        data_in_bytes = str.encode(data)
        data_begin = 0
        if len(data) < 512:
            data_end = len(data)
        else:
            data_end = 512 

        while True:
            msg, addr = client_socket.recvfrom(516)  # 4 bytes for header, 512 max data size according to RFC

            if msg[0] != 0 and msg[1] not in range(1,6):  # check opcode in TFTP packet
                print('Unknown TFTP opcode')
                continue

            if addr[1] != server_tid:  # check server's TID for case when first packet from server got duplicated somewhere in the network
                if server_tid == 69:
                    server_tid = addr[1]
                else:
                    continue

            print('TFTP opcode:', msg[1])

            if msg[1] == 5:  # error received
                print('Error code:', msg[3], error_codes[msg[3]])
                print('Error message:', msg[4:])
                if msg[3] == 5:  # unknown TID errors don't terminate the connection
                    continue
                else:
                    break

            if msg[1] == 4:  # ack received
                block_number = msg[2] + msg[3]
                print('Block number received:', block_number)
                block_number += 1
                reply = bytes([0, 3]) +  (block_number).to_bytes(2, byteorder='big') + data_in_bytes[data_begin:data_end]
                print('Block number sent:', block_number)

                if show_details:
                    print('Reply:\n', reply)

                client_socket.sendto(reply, addr)
                if len(reply) < 516:
                    break
                else:
                    data_begin += 512
                    if len(data) - data_begin < 512:
                        data_end = len(data)
                    else:
                        data_end += 512

    else:
        print('Unknown command: {}'.format(cmd))