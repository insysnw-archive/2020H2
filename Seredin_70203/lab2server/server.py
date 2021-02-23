import socket
import argparse
import struct

# DHCP message types
DHCPDISCOVER = 1
DHCPOFFER = 2
DHCPREQUEST = 3
DHCPDECLINE = 4
DHCPACK = 5
DHCPNAK = 6
DHCPRELEASE = 7

host_address = '0.0.0.0'
server_port = 67
subnet_mask = '255.255.255.0'
router_address = '192.168.0.1'
domain_name_server = '192.168.0.1'
assigned_addresses = []

class DHCPPacket:
    def __init__(self):
        self.opcode = bytearray([2])
        self.htype = bytearray([1])
        self.hlen = bytearray([6])
        self.hops = bytearray([0])
        self.tid = bytearray(4)
        self.secs = bytearray(2)
        self.flags = bytearray(2)
        self.caddr = bytearray(4)
        self.yaddr = bytearray(4)
        self.saddr = bytearray([0, 0, 0, 0])
        self.gaddr = bytearray([0, 0, 0, 0])
        self.chaddr = bytearray(16)
        self.sname = bytearray(64)
        self.file = bytearray(128)
        self.options = bytearray(312)
        self.options[:4] = [99, 130, 83, 99]  # magic cookie
        self.options[4:7] = [53, 1, 2]  # DHCP message type, option code 53, length 1, type 2 (DHCPOFFER) by default
        self.options[7:13] = bytearray([54, 4]) + bytearray([int(byte) for byte in host_address.split('.')]) # DHCP server i server IP address
        self.options[13:19] = [51, 4, 255, 255, 255, 255]  # IP address lease time, code 51, len 4, time in seconds (maximum by default)
        self.options[19:25] = bytearray([1, 4]) + bytearray([int(byte) for byte in subnet_mask.split('.')]) # subnet mask, code 1, len 4, mask
        self.options[25:31] = bytearray([3, 4]) + bytearray([int(byte) for byte in router_address.split('.')]) # router, code 3, len = 4 * number_of_routers, assuming only one router on subnet with default address 192.168.0.1
        self.options[31:37] = bytearray([6, 4]) + bytearray([int(byte) for byte in domain_name_server.split('.')]) # domain name server, code 6, len = 4 * number_of_servers, assuming one server with default address 192.168.0.1
        self.options[37:38] = [255]  # end option, code 255, default len 1, subsequent options are pad options

    def form_packet(self):
        return self.opcode + self.htype + self.hlen + self.hops + self.tid + self.secs + self.flags + self.caddr + self.yaddr \
        + self.saddr + self.gaddr + self.chaddr + self.sname + self.file + self.options

def get_addr():
    current_free_addr = bytearray([int(byte) for byte in host_address.split('.')])
    while current_free_addr[3] <= 255:
        current_free_addr[3] = current_free_addr[3] + 1
        if current_free_addr not in assigned_addresses:
            return current_free_addr

def packet_send(message):
    packet = DHCPPacket()

    packet.tid = message[4:8]
    packet.flags = message[10:12]
    packet.caddr = message[12:16]
    packet.gaddr = message[24:28]
    packet.chaddr = message[28:44]

    if message[243] == 50 and message[245:249] != bytearray([0, 0, 0, 0]) and message[245:249] not in assigned_addresses:
        packet.yaddr = message[245:249]
    else:
        packet.yaddr = get_addr()

    if message[242] == DHCPDISCOVER:
        packet.options[6] = 2
        print('offered address: ', [byte for byte in packet.yaddr])

    if message[242] == DHCPREQUEST:
        packet.options[6] = 5  # DHCPACK
        assigned_addresses.append(packet.yaddr)
        print('assigned address: ', [byte for byte in packet.yaddr])

    return packet.form_packet()

print('DHCP server running at', host_address,':', server_port)
server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
server_socket.bind((host_address, server_port))

while True:
    message, addr = server_socket.recvfrom(1024)

    print('message received from: ', addr)
    print('message type:', message[242])

    if message[242] == DHCPDISCOVER or message[242] == DHCPREQUEST:

        if message[242] == DHCPDISCOVER:
            print('DHCPDISCOVER accepted')
            print('send DHCPOFFER to: ', addr)

        elif message[242] == DHCPREQUEST:
            print('DHCPREQUEST accepted')
            print('send DHCPACK to: ', addr)

        reply = packet_send(message)
        server_socket.sendto(reply, addr)

    if message[242] == DHCPDECLINE:
        print('DHCPDECLINE accepted')
        print('some problems')

    if message[242] == DHCPRELEASE:
        print('DHCPRELEASE accepted')
        assigned_addresses.remove(message[12:16])
        print([byte for byte in message[12:16]], 'was removed')