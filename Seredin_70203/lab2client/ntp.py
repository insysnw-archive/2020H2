import datetime
import time
import socket
import struct

FORMAT = (datetime.date(1970, 1, 1) - datetime.date(1900, 1, 1)).days * 24 * 3600
WAITNG = 10
SERVER = "pool.ntp.org"
PORT = 123
	
class Packet:

    pack_format = "!B B b b 11I"

    def __init__(self, version=2, mode=3, transmit=0):
        self.li= 0
        self.vn = version 
        self.mode = mode 
        self.stratum = 0 
        self.poll = 0 
        self.precision = 0 
        self.root_delay = 0 
        self.root_dispersion = 0  
        self.reference_identifier = 0
        self.reference_timestamp = 0
        self.originate_timestamp = 0
        self.receive_timestamp = 0
        self.transmit_timestamp = transmit

    def pack(self):
        return struct.pack(Packet.pack_format,
                (self.li << 6) + (self.vn << 3) + self.mode, 
                self.stratum,
                self.poll,
                self.precision,
                int(self.root_delay) + get_fraction(self.root_delay, 16), 
                int(self.root_dispersion) + get_fraction(self.root_dispersion, 16), 
                self.reference_identifier,
                int(self.reference_timestamp), #I Integer part
                get_fraction(self.reference_timestamp, 32), #I Fractional part
                int(self.originate_timestamp),#I
                get_fraction(self.originate_timestamp, 32), #I
                int(self.receive_timestamp), #I
                get_fraction(self.receive_timestamp, 32), #I
                int(self.transmit_timestamp), #I
                get_fraction(self.transmit_timestamp, 32)) #I

    def unpack(self, data: bytes):
        unpacked_data = struct.unpack(Packet.pack_format, data)

        self.li = unpacked_data[0] >> 6  
        self.vn = unpacked_data[0] >> 3 & 0b111 
        self.mode = unpacked_data[0] & 0b111  

        self.stratum = unpacked_data[1]  
        self.poll = unpacked_data[2]  
        self.precision = unpacked_data[3] 

        self.root_delay = (unpacked_data[4] >> 16) + \
            (unpacked_data[4] & 0xFFFF) / 2 ** 16
        self.root_dispersion = (unpacked_data[5] >> 16) + \
            (unpacked_data[5] & 0xFFFF) / 2 ** 16 

        self.reference_identifier = str((unpacked_data[6] >> 24) & 0xFF) + " " + \
                      str((unpacked_data[6] >> 16) & 0xFF) + " " +  \
                      str((unpacked_data[6] >> 8) & 0xFF) + " " +  \
                      str(unpacked_data[6] & 0xFF)

        self.reference_timestamp = unpacked_data[7] + unpacked_data[8] / 2 ** 32
        self.originate_timestamp = unpacked_data[9] + unpacked_data[10] / 2 ** 32
        self.receive_timestamp = unpacked_data[11] + unpacked_data[12] / 2 ** 32
        self.transmit_timestamp = unpacked_data[13] + unpacked_data[14] / 2 ** 32
        return self

    def get_result(self, time_different):
        return "Server time: {0} \n"\
			"Time difference: {1}\n"\
			"Leap indicator: {2.li}\n" \
            "Version number: {2.vn}\n" \
            "Mode: {2.mode}\n" \
            "Stratum: {2.stratum}\n" \
            "Pool: {2.poll}\n" \
            "Precision: {2.precision}\n" \
            "Root delay: {2.root_delay}\n" \
            "Root dispersion: {2.root_dispersion}\n" \
            "Ref id: {2.reference_identifier}\n" \
            "Reference: {2.reference_timestamp}\n" \
            "Originate: {2.originate_timestamp}\n" \
            "Receive: {2.receive_timestamp}\n" \
            "Transmit: {2.transmit_timestamp}"\
            .format(datetime.datetime.fromtimestamp(time.time() + time_different).strftime("%c"),time_different,self)

def get_fraction(number, precision):
    return int((number - int(number)) * 2 ** precision)
	
def get_information():   
	packet = Packet(version=2, mode=3, transmit=time.time() + FORMAT)
	answer = Packet()
	with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
	    s.settimeout(WAITNG)
	    s.sendto(packet.pack(), (SERVER, PORT))
	    data = s.recv(48)
	    arrive_time = time.time() + FORMAT
	    answer.unpack(data)

	time_different = answer.receive_timestamp-answer.originate_timestamp-arrive_time+answer.transmit_timestamp
	return answer.get_result(time_different)

def main():
	print("time or exit")
	while(True):
		user_input=input("\nCommand: ")
		if user_input == "time":
			print(get_information())
		elif user_input=="exit":
			exit()

if __name__=='__main__':
	main() 