import struct

class NTP_Packet:

    pack_format = "!B B b b 11I" # "!"- big endian, standard type sizes, 
                                 #B -unsigned char, signed char, I - unsigned int

    def __init__(self, version=2, mode=3, transmit=0):
        
        self.li= 0 # Info about enter leap second
       
        self.version = version  # Protocol version
        
        self.mode = mode # Mode of packet sender (3 = client)
        
        self.stratum = 0 # The number of layers to server with reference_time time
        
        self.pool = 0 # Expected interval between requests
        
        self.precision = 0 # Precision (log2)
        
        self.root_delay = 0 # Interval for time to reach NTP-server
       
        self.root_dispersion = 0  # NTP-server dispersion
       
        self.ref_id = 0  # Id of server's source of time
        
        self.reference_time = 0 # Last time on server
       
        self.originate_time = 0  # Time of sending packet to server
        
        self.receive_time = 0 # Time of receiving packet on server
       
        self.transmit_time = transmit  # Time of sending answer from server

    def pack(self):
        return struct.pack(NTP_Packet.pack_format,
                (self.li << 6) + (self.version << 3) + self.mode, #B
                self.stratum, #B
                self.pool, #b
                self.precision, #b
                int(self.root_delay) + get_fraction(self.root_delay, 16), #I
                int(self.root_dispersion) + get_fraction(self.root_dispersion, 16), #I
                self.ref_id, #I
                int(self.reference_time), #I Integer part
                get_fraction(self.reference_time, 32), #I Fractional part
                int(self.originate_time),#I
                get_fraction(self.originate_time, 32), #I
                int(self.receive_time), #I
                get_fraction(self.receive_time, 32), #I
                int(self.transmit_time), #I
                get_fraction(self.transmit_time, 32)) #I

    def unpack(self, data: bytes):
        unpacked_data = struct.unpack(NTP_Packet.pack_format, data)

        self.li = unpacked_data[0] >> 6  # 2 bits
        self.version = unpacked_data[0] >> 3 & 0b111  # 3 bits
        self.mode = unpacked_data[0] & 0b111  # 3 bits

        self.stratum = unpacked_data[1]  # 1 byte
        self.pool = unpacked_data[2]  # 1 byte
        self.precision = unpacked_data[3]  # 1 byte

        # 2 bytes
        self.root_delay = (unpacked_data[4] >> 16) + \
            (unpacked_data[4] & 0xFFFF) / 2 ** 16
         # 2 bytes
        self.root_dispersion = (unpacked_data[5] >> 16) + \
            (unpacked_data[5] & 0xFFFF) / 2 ** 16 

        # 4 bytes
        self.ref_id = str((unpacked_data[6] >> 24) & 0xFF) + " " + \
                      str((unpacked_data[6] >> 16) & 0xFF) + " " +  \
                      str((unpacked_data[6] >> 8) & 0xFF) + " " +  \
                      str(unpacked_data[6] & 0xFF)

        self.reference_time = unpacked_data[7] + unpacked_data[8] / 2 ** 32  # 8 bytes
        self.originate_time = unpacked_data[9] + unpacked_data[10] / 2 ** 32  # 8 bytes
        self.receive_time = unpacked_data[11] + unpacked_data[12] / 2 ** 32  # 8 bytes
        self.transmit_time = unpacked_data[13] + unpacked_data[14] / 2 ** 32  # 8 bytes

        return self

def get_fraction(number, precision):
    return int((number - int(number)) * 2 ** precision)