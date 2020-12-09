import struct

class DNS_Header:

    pack_format = "!6H" # "!"- big endian, standard type sizes, #H -unsigned short

    def __init__(self):
        
        self.id= 0 # Transation id (16 bits)

        self.qr= 0 # Type of message (0-query, 1-responce) (1 bit)

        self.opcode= 0 # Type of query (0 - standard query, 1 - inverse query, 
                       # 2 - server status request) (4 bit)

        self.aa= 0 # Authorative answer (1 bit)

        self.tc= 0 # Was message truncated (1 bit)

        self.rd= 0 # Recurtion desired by client (1 bit)

        self.ra= 0 # Recurtion avaliable on server (1 bit)

        self.rcode= 0 # Responce code (0 - no error) (4 bit)

        self.qdcount= 0 # Number of entries in the query section (16 bits)

        self.ancount= 0 # Number of resource records in the answer section (16 bits)

        self.nscount= 0 # Number of name server resource records in the authority section (16 bits)

        self.arcount= 0 # Number of resource records in the additional records section (16 bits)
       
    def pack(self):
        return struct.pack(DNS_Header.pack_format,
                self.id,
                (self.qr << 15) + (self.opcode << 11) + (self.aa << 10) + (self.tc << 9)
                  + (self.rd << 8) + (self.ra << 7) + self.rcode,
                self.qdcount,
                self.ancount,
                self.nscount,
                self.arcount) 

    def unpack(self, data: bytes):
        unpacked_data = struct.unpack(DNS_Header.pack_format, data)

        self.id = unpacked_data[0]
        self.qr = unpacked_data[1] >> 15
        self.opcode = unpacked_data[1]>>11 & 0b1111
        self.aa = unpacked_data[1]>>10 & 0b1
        self.tc = unpacked_data[1]>>9 & 0b1
        self.rd = unpacked_data[1]>>8 & 0b1
        self.ra = unpacked_data[1]>>7 & 0b1
        self.ra = unpacked_data[1] & 0b1
        self.qdcount = unpacked_data[2]
        self.ancount = unpacked_data[3]
        self.nscount = unpacked_data[4]
        self.arcount = unpacked_data[5]

        return self

class DNS_Answer:

    
    def __init__(self):
        
        self.name= 0 # Domain name, represented as a sequence of lables (64 bits)

        self._type= 0 # Resource record type (16 bits)

        self._class= 0 # Class of the data in the RDATA field (16 bits)

        self.ttl= 0 # Time to live is seconds (32 bits)

        self.rdlength= 0 # Length in bytes of the RDATA field (16 bits)

        self.txtlength= 0 # Field for TXT records

        self.preference=0 # Field for MX records

        self.rdata= 0 # Resourse data (32 bits)
       
    def pack(self, req_type):

        if req_type==1 or req_type==28:
            pack_format = f"!H H H I H {self.rdlength}s" # "!"- big endian, standard type sizes, 
                                 #B -unsigned char, signed char, I - unsigned int
            return struct.pack(pack_format,
                self.name, 
                self._type,
                self._class,
                self.ttl,
                self.rdlength,
                self.rdata)

        elif req_type==16:
            pack_format = f"!H H H I H B {self.txtlength}s" # "!"- big endian, standard type sizes, 
                                 #B -unsigned char, signed char, I - unsigned int
            return struct.pack(pack_format,
                self.name, 
                self._type,
                self._class,
                self.ttl,
                self.rdlength,
                self.txtlength,
                self.rdata)

        elif req_type==15:
            pack_format = f"!H H H I H H {self.rdlength-2}s" # "!"- big endian, standard type sizes, 
                                 #B -unsigned char, signed char, I - unsigned int
            return struct.pack(pack_format,
                self.name, 
                self._type,
                self._class,
                self.ttl,
                self.rdlength,
                self.preference,
                self.rdata)

    def unpack(self, data: bytes):
        unpacked_data = struct.unpack(DNS_Answer.pack_format, data)

        self.name = unpacked_data[0]
        self._type = unpacked_data[1]
        self._class = unpacked_data[2]
        self.ttl = unpacked_data[3]
        self.rdlength = unpacked_data[4]
        self.rdata = unpacked_data[5]

        return self
        