import argparse
import os
import sys
from datetime import datetime
from math import modf
import socket
from struct import pack
from struct import unpack

# Время в Unix начинается с 1 Января 1970 в секундах это 2208988800
SEVENTY_YEARS = 2208988800


def NTPtime_to_datetime(int_val, fract_val):
    ntp_time = int_val + fract_val / 2 ** 32
    return datetime.fromtimestamp(ntp_time - SEVENTY_YEARS)


def datetime_to_NTPtime(dt):
    ntp_ts_frac, ntp_ts_int = modf(dt.timestamp() + SEVENTY_YEARS)
    ntp_ts_frac = int(ntp_ts_frac * 2**32)
    ntp_ts_int = int(ntp_ts_int)

    return ntp_ts_frac, ntp_ts_int


class NTPClient(object):
    LEAP_ATR = {
        0: "No warning",
        1: "Last minute of the day has 61 seconds",
        2: "Last minute of the day has 59 seconds",
        3: "Unknown (clock is not synchronized)"
    }

    # Mode of packet's sender
    MODE_ATR = {
        0: "Reserved",
        1: "Symmetric active",
        2: "Symmetric passive",
        3: "Client",
        4: "Server",
        5: "Broadcast",
        6: "Reserved for NTP control messages",
        7: "Reserved for private use",
    }

    def __init__(self, server, timeout, port=123):
        own_ts_frac, own_ts_int = datetime_to_NTPtime(datetime.now())

        ref_ts = pack(
            '!4B11I',
            int('11100011', 2),  # LI=11(unknown), VN=100(v4), MODE=011(client)
            0,  # Stratum - часовой слой
            0,  # Poll Interval - интервал опроса
            0,  # Precision - точность
            0,  # Root Delay - задержка
            0,  # Root Dispersion - дисперсия
            0,  # Reference Identifier - идентификатор источника
            0,  # Reference Timestamp (integer part) - время обновления
            0,  # Reference Timestamp (fraction part)
            0,  # Originate Timestamp (integer part) - время клиента, когда запрос отправляется серверу
            0,  # Originate Timestamp (fraction part)
            0,  # Receive Timestamp (integer part) - время сервера, когда пришло сообщение от клиента
            0,  # Receive Timestamp (fraction part)
            own_ts_int,  # Transmit Timestamp (integer part)
            own_ts_frac,  # Transmit Timestamp (fraction part)
        )

        # send request to NTP server
        self.local_transmit_ts = datetime.now()

        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.sendto(ref_ts, (server, port))
            s.settimeout(timeout)
            try:
                response = s.recvfrom(1024)[0]
            except:
                print('Timeout! Try to connect to the server again')
                os._exit(0)
        self.local_receive_ts = datetime.now()

        # parse response
        if response:
            unpacked = unpack('!4B11I', response)
            first_oct = format(unpacked[0], 'b').zfill(8)

            leap = int(first_oct[0:2], 2)

            self.leap_indicator = leap
            self.leap_indicator_atr = self.LEAP_ATR[leap] if leap in self.LEAP_ATR else 'Unknown'

            self.version_number = int(first_oct[2:5], 2)

            mode = int(first_oct[5:8], 2)
            self.mode = mode
            self.mode_atr = self.MODE_ATR[mode] if mode in self.MODE_ATR else 'Unknown'

            self.stratum = unpacked[1]
            self.poll_interval = unpacked[2]
            self.precision = unpacked[3]
            self.root_delay = unpacked[4]
            self.root_dispersion = unpacked[5]
            self.reference_identifier = unpacked[6]
            self.reference_ts = NTPtime_to_datetime(unpacked[7], unpacked[8])
            self.originate_ts = NTPtime_to_datetime(unpacked[9], unpacked[10])
            self.receive_ts = NTPtime_to_datetime(unpacked[11], unpacked[12])
            self.transmit_ts = NTPtime_to_datetime(unpacked[13], unpacked[14])
            self.rtt = (self.local_receive_ts - self.local_transmit_ts).total_seconds() - (self.transmit_ts - self.receive_ts).total_seconds()
            self.offset = ((self.transmit_ts - self.local_receive_ts).total_seconds() + (self.receive_ts - self.local_transmit_ts).total_seconds()) * 0.5


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-s', '--server', type=str, default='pool.ntp.org')
    parser.add_argument('-t', '--timeout', type=int, default=10)
    args = parser.parse_args(sys.argv[1:])
    print(f'Server: {args.server}')
    print('--------------------------------------')

    ntp = NTPClient(args.server, args.timeout)

    print(f"Server time: {ntp.transmit_ts}")
    print(f"Local time: {ntp.local_transmit_ts}")