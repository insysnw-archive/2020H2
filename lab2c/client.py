import socket
import datetime
import time
import struct
import sys

#ntp_ip = "pool.ntp.org"

if len(sys.argv) != 3:
    print("python3 client.py [Server IP] [Server port]")
    sys.exit()

ntp_ip = sys.argv[1]

ntp_port = int(sys.argv[2]) #123

class NTPPacket:

    _format = "!B B b b 11I"

    def __init__(self, version_number=2, mode=3,transmit=0):
        # Индикатор коррекции (2 bits)
        self.leap_indicator = 0
        # Версия протокола (3 bits)
        self.version_number = version_number
        # Режим работы отправителя пакета (3 bits)
        self.mode = mode
        # Уровень наслоения (1 byte)
        self.stratum = 0
        # Интервал между запросами (1 byte)
        self.pool = 0
        # Точность (log2) (1 byte)
        self.precision = 0
        # Задержка сервера (4 bytes)
        self.root_delay = 0
        # Разброс показаний сервера (4 bytes)
        self.root_dispersion = 0
        # Идентификатор часов (4 bytes)
        self.ref_id = 0
        # Последние показания часов на сервере (8 bytes)
        self.reference = 0
        # Время отправления пакета (8 bytes)
        self.originate = 0
        # Время получения пакета (8 bytes)
        self.receive = 0
        # Время отправки пакета с сервера (8 bytes)
        self.transmit = transmit

    def pack(self):
        packet = struct.pack(NTPPacket._format, (self.leap_indicator << 6) + \
                (self.version_number << 3) + self.mode,
                self.stratum,
                self.pool,
                self.precision,
                int(self.root_delay) + self.get_fraction(self.root_delay, 16),
                int(self.root_dispersion) + 
                    self.get_fraction(self.root_dispersion, 16),
                self.ref_id,
                int(self.reference),
                self.get_fraction(self.reference, 32),
                int(self.originate),
                self.get_fraction(self.originate, 32),
                int(self.receive),
                self.get_fraction(self.receive, 32),
                int(self.transmit),
                self.get_fraction(self.transmit, 32))
        return packet

    def get_fraction(self, number, precision):
        return int((number - int(number)) * 2 ** precision)
        

    def unpack(self, data: bytes):
        unpacked_data = struct.unpack(NTPPacket._format,data)

        self.leap_indicator = data[0] >> 6  # 2 bits
        self.version_number = data[0] >> 3 & 0b111  # 3 bits
        self.mode = data[0] & 0b111  # 3 bits

        self.stratum = data[1]  # 1 byte
        self.pool = data[2]  # 1 byte
        self.precision = data[3]  # 1 byte

         # 4 bytes
        self.root_delay = (unpacked_data[4] >> 16) + \
            (unpacked_data[4] & 0xFFFF) / 2 ** 16
         # 4 bytes
        self.root_dispersion = (unpacked_data[5] >> 16) + \
            (unpacked_data[5] & 0xFFFF) / 2 ** 16 

        # 4 bytes
        self.ref_id = str((unpacked_data[6] >> 24) & 0xFF) + " " + \
                      str((unpacked_data[6] >> 16) & 0xFF) + " " +  \
                      str((unpacked_data[6] >> 8) & 0xFF) + " " +  \
                      str(unpacked_data[6] & 0xFF)

        self.reference = unpacked_data[7] + unpacked_data[8] / 2 ** 32  # 8 bytes
        self.originate = unpacked_data[9] + unpacked_data[10] / 2 ** 32  # 8 bytes
        self.receive = unpacked_data[11] + unpacked_data[12] / 2 ** 32  # 8 bytes
        self.transmit = unpacked_data[13] + unpacked_data[14] / 2 ** 32  # 8 bytes
        
        return self


    def display(self):
        return "Leap indicator: {0.leap_indicator}\n" \
                "Version number: {0.version_number}\n" \
                "Mode: {0.mode}\n" \
                "Stratum: {0.stratum}\n" \
                "Pool: {0.pool}\n" \
                "Precision: {0.precision}\n" \
                "Root delay: {0.root_delay}\n" \
                "Root dispersion: {0.root_dispersion}\n" \
                "Ref id: {0.ref_id}\n" \
                "Reference: {0.reference}\n" \
                "Originate: {0.originate}\n" \
                "Receive: {0.receive}\n" \
                "Transmit: {0.transmit}"\
                .format(self)


class NTPClient:
    
    FORMAT_DIFF = (datetime.date(1970, 1, 1) - datetime.date(1900, 1, 1)).days * 24 * 3600

    def __init__(self, ntp_ip, ntp_port, waiting_time=5):
        self.ip = ntp_ip
        self.port = ntp_port
        self.waiting = waiting_time


    def get_time(self):
        current_time = time.time() + self.FORMAT_DIFF
        request = NTPPacket(version_number=2, mode=3, transmit=current_time)
        response = NTPPacket()
        time_of_arrival = 0
        #print(request.display())

        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.settimeout(self.waiting)
            s.sendto(request.pack(), (self.ip, self.port))
            data = s.recv(48)

            time_of_arrival = time.time() + self.FORMAT_DIFF
            response.unpack(data)
            #print(response.display())

        delta = response.receive - response.originate - (time_of_arrival - \
        	response.originate - response.transmit + response.receive)/2

        return datetime.datetime.fromtimestamp(time.time() + delta).strftime("%c")



def main():
    client = NTPClient(ntp_ip,ntp_port)
    while True:
        print(client.get_time())
        time.sleep(10)


if __name__ == '__main__':
    main()

