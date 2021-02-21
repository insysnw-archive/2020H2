import datetime
import socket
import struct
import time
import select
from _thread import start_new_thread
from ntp_packet import NTP_Packet

_SYSTEM_EPOCH = datetime.date(*time.gmtime(0)[0:3])
_NTP_EPOCH = datetime.date(1900, 1, 1)

#delta between system and NTP time"""
NTP_DELTA = (_SYSTEM_EPOCH - _NTP_EPOCH).days * 24 * 3600

IP = "0.0.0.0"
port = 123

#Convert a system time to a NTP time.
def system_to_ntp_time(timestamp):

    return timestamp + NTP_DELTA

def make_resp(data, addr, recvTimestamp):

    recvPacket = NTP_Packet()
    recvPacket.unpack(data)

    respPacket = NTP_Packet()
    respPacket.version = 0x3
    respPacket.mode = 0x4
    respPacket.stratum = 0x2
    respPacket.pool = 3
    respPacket.precision = -25
    respPacket.root_delay = 0x0bfa
    respPacket.root_dispersion = 0x06a7
    respPacket.ref_id = 0xa29fc801
    respPacket.reference_time = recvTimestamp-10000
    respPacket.originate_time = recvPacket.transmit_time
    respPacket.receive_time =recvTimestamp-10000
    respPacket.transmit_time = system_to_ntp_time(time.time())-10000

    socket.sendto(respPacket.pack(),addr)
    print(f"Sended to {addr[0]}:{addr[1]}")

                  
socket = socket.socket(socket.AF_INET,socket.SOCK_DGRAM) #IPv4, UDP
socket.bind((IP,port))

print(f'Listening for connections on {IP}:{port}...')


while True:

    rlist,wlist,elist = select.select([socket],[],[],1);
    if len(rlist) != 0:
        print(f"Receive {len(rlist)} packets")
        for tempSocket in rlist:
            try:
                data,addr = tempSocket.recvfrom(48)
                recvTimestamp = system_to_ntp_time(time.time())
                start_new_thread(make_resp,(data, addr, recvTimestamp))
            except e:
                print(e.message)
    