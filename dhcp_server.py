import socket
import argparse


# DHCP message types
DHCPDISCOVER = 1
DHCPOFFER = 2
DHCPREQUEST = 3
DHCPDECLINE = 4
DHCPACK = 5
DHCPNAK = 6
DHCPRELEASE = 7


parser = argparse.ArgumentParser(description='DHCP server')
parser.add_argument("--ip", default='0.0.0.0', type=str, help='ip address at wich server will receive messages')
parser.add_argument("--port", default=67, type=int, help='port at wich server will receive messages')
parser.add_argument("--mask", default='255.255.255.0', type=str, help='subnet mask')
parser.add_argument("--router", default='192.168.0.1', type=str, help='router address')
parser.add_argument("--domain", default='192.168.0.1', type=str, help='domain name server')
parser.add_argument("-d", default=0, type=bool, help='show details: 1 to show all incoming and outcoming messages')
args = parser.parse_args()
host_address = args.ip
server_port = args.port
subnet_mask = args.mask
router_address = args.router
domain_name_server = args.domain
show_details = args.d


assigned_addresses = []


# get address to offer/assign
def get_yiaddr():
    current_free_yiaddr = bytearray([int(byte) for byte in host_address.split('.')])
    while current_free_yiaddr[3] <= 255:
        current_free_yiaddr[3] = current_free_yiaddr[3] + 1
        if current_free_yiaddr not in assigned_addresses:
            return current_free_yiaddr



class DHCPPacket:
    def __init__(self):
        self.op = bytearray([2])  # message type, 2 for reply
        self.htype = bytearray([1])  # hardware address type, 1 for 10mb ethernet by default
        self.hlen = bytearray([6])  # hardware address length, 6 for 10mb ethernet by default
        self.hops = bytearray([0])  # used by relay agents, client sets to 0
        self.xid = bytearray(4)  # transaction ID, random number chosen by client
        self.secs = bytearray(2)  # seconds elapsed, filled in by client
        self.flags = bytearray(2)  # flags from client's DHCPDISCOVER or DHCPREQUEST
        self.ciaddr = bytearray(4)  # client's IP address
        self.yiaddr = bytearray(4)  # available network address for client
        self.siaddr = bytearray([0, 0, 0, 0])  # IP address of next server to use, 0.0.0.0 by default
        self.giaddr = bytearray([0, 0, 0, 0])  # relay agent IP address, 0.0.0.0 by default
        self.chaddr = bytearray(16)  # client hardware address
        self.sname = bytearray(64)  # optional server host name
        self.file = bytearray(128)  # boot file name, 0 by default
        self.options = bytearray(312)
        self.options[:4] = [99, 130, 83, 99]  # magic cookie
        self.options[4:7] = [53, 1, 2]  # DHCP message type, option code 53, length 1, type 2 (DHCPOFFER) by default
        self.options[7:13] = bytearray([54, 4]) + bytearray([int(byte) for byte in host_address.split('.')]) # DHCP server identifier, option code 54, length 4, server IP address
        self.options[13:19] = [51, 4, 255, 255, 255, 255]  # IP address lease time, code 51, len 4, time in seconds (maximum by default)
        self.options[19:25] = bytearray([1, 4]) + bytearray([int(byte) for byte in subnet_mask.split('.')]) # subnet mask, code 1, len 4, mask
        self.options[25:31] = bytearray([3, 4]) + bytearray([int(byte) for byte in router_address.split('.')]) # router, code 3, len = 4 * number_of_routers, assuming only one router on subnet with default address 192.168.0.1
        self.options[31:37] = bytearray([6, 4]) + bytearray([int(byte) for byte in domain_name_server.split('.')]) # domain name server, code 6, len = 4 * number_of_servers, assuming one server with default address 192.168.0.1 
        self.options[37:38] = [255]  # end option, code 255, default len 1, subsequent options are pad options


    # combine DHCP packet fields in one bytearray
    def form_packet(self):
        return self.op + self.htype + self.hlen + self.hops + self.xid + self.secs + self.flags + self.ciaddr + self.yiaddr \
        + self.siaddr + self.giaddr + self.chaddr + self.sname + self.file + self.options


# creating and binding UDP-socket
print('DHCP server starting at', host_address,':', server_port)
server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
server_socket.bind((host_address, server_port))


# get DHCP reply packet depending on incoming DHCP message type
def get_dhcp_packet(msg):
    packet = DHCPPacket()

    packet.xid = msg[4:8]
    print('extracted xid: ', packet.xid)
    packet.flags = msg[10:12]
    print('extracted flags: ', packet.flags)
    packet.ciaddr = msg[12:16]
    print('extracted ciaddr: ', packet.ciaddr)
    packet.giaddr = msg[24:28]
    print('extracted giaddr: ', packet.giaddr)
    packet.chaddr = msg[28:44]
    print('extracted chaddr: ', packet.chaddr)

    # check for requested address option field
    if msg[243] == 50 and msg[245:249] != bytearray([0, 0, 0, 0]) and msg[245:249] not in assigned_addresses:
        packet.yiaddr = msg[245:249]
    else:
        packet.yiaddr = get_yiaddr()

    if msg[242] == DHCPDISCOVER:
        packet.options[6] = 2  # DHCPOFFER
        print('offered yiaddr: ', [int(byte) for byte in packet.yiaddr])

    if msg[242] == DHCPREQUEST:
        packet.options[6] = 5  # DHCPACK
        assigned_addresses.append(packet.yiaddr)
        print('yiaddr to assign: ', [int(byte) for byte in packet.yiaddr])
  
    return packet.form_packet()


while True:
    msg, addr = server_socket.recvfrom(1024)

    if show_details:
        print('INCOMING MESSAGE:')
        print([byte for byte in msg])

    print('MESSAGE RECEIVED FROM: ', addr)
    print('MESSAGE TYPE:', msg[242])

    if msg[242] == DHCPDISCOVER or msg[242] == DHCPREQUEST:

        if msg[242] == DHCPDISCOVER:
            print('DHCPDISCOVER ACCEPTED')
            print('SENDING DHCPOFFER TO ', addr)
            
        elif msg[242] == DHCPREQUEST:
            print('DHCPREQUEST ACCEPTED')
            print('SENDING DHCPACK TO ', addr)

        reply = get_dhcp_packet(msg)
        server_socket.sendto(reply, addr)

        if show_details:
            print('OUTCOMING MESSAGE:')
            print([byte for byte in reply])

    if msg[242] == DHCPDECLINE:
        print('DHCPDECLINE ACCEPTED')
        print('possible configuration problem')

    if msg[242] == DHCPRELEASE:
        print('DHCPRELEASE ACCEPTED')
        assigned_addresses.remove(msg[12:16])  # removing client address from assigned addresses  
        print([byte for byte in msg[12:16]], 'removed from assigned addresses')   
