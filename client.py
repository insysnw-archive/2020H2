import socket
import datetime
import time
import struct
import sys

if len(sys.argv) != 3:
    print("Specify IP and port")
    sys.exit()

ip = sys.argv[1]
port = int(sys.argv[2])


class NTPPacket:
    _format = "!B B b b 11I"

    def __init__(self, version_number=2, mode=3, transmit=0):
        self.leap_indicator = 0  # Индикатор коррекции (2 bits)
        self.version_number = version_number  # Номер версии (3 bits)
        self.mode = mode  # Режим работы (3 bits)
        self.stratum = 0  # Уровень наслоения (1 byte)
        self.pool = 0  # Максимальны интервал между сообщениями (1 byte)
        self.precision = 0  # Точность (1 byte)
        self.root_delay = 0  # Задержка сервера (4 bytes)
        self.root_dispersion = 0  # Разброс показаний сервера (4 bytes)
        self.ref_id = 0  # Идентификатор источника (4 bytes)
        self.reference = 0  # Последние показания часов на сервере (8 bytes)
        self.originate = 0  # Время, когда пакет был отправлен (8 bytes)
        self.receive = 0  # Время получения пакета (8 bytes)
        self.transmit = transmit  # Время отправки пакета с сервера (8 bytes)

    def get_fraction(self, number, precision):
        return int((number - int(number)) * 2 ** precision)

    def pack(self):
        packed_data = struct.pack(NTPPacket._format,
                           (self.leap_indicator << 6) +
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
        return packed_data

    def unpack(self, data: bytes):
        unpacked_data = struct.unpack(NTPPacket._format, data)

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
                      str((unpacked_data[6] >> 16) & 0xFF) + " " + \
                      str((unpacked_data[6] >> 8) & 0xFF) + " " + \
                      str(unpacked_data[6] & 0xFF)

        self.reference = unpacked_data[7] + unpacked_data[8] / 2 ** 32  # 8 bytes
        self.originate = unpacked_data[9] + unpacked_data[10] / 2 ** 32  # 8 bytes
        self.receive = unpacked_data[11] + unpacked_data[12] / 2 ** 32  # 8 bytes
        self.transmit = unpacked_data[13] + unpacked_data[14] / 2 ** 32  # 8 bytes

        return self


class NTPClient:
    FORMAT_DIFF = (datetime.date(1970, 1, 1) - datetime.date(1900, 1, 1)).days * 24 * 3600

    def __init__(self, ip, port, waiting_time=5,):
        self.ip = ip
        self.port = port
        self.waiting = waiting_time

    def get_time(self):
        current_time = time.time() + self.FORMAT_DIFF
        request = NTPPacket(version_number=2, mode=3, transmit=current_time)
        reply = NTPPacket()
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.settimeout(self.waiting)
            s.sendto(request.pack(), (self.ip, self.port))
            data = s.recv(48)
            arrive_time = time.time() + self.FORMAT_DIFF
            reply.unpack(data)
        delta = reply.receive - reply.originate - (arrive_time -
                                                         reply.originate - reply.transmit + reply.receive) / 2

        return datetime.datetime.fromtimestamp(time.time() + delta).strftime("%c")


def main():
    client = NTPClient(ip, port)
    try:
        while True:
            request = input("Enter time to get time ")
            if request == "time":
                print(client.get_time())
    except Exception as e:
        print("Exception: " + str(e))
        sys.exit()


if __name__ == '__main__':
    main()