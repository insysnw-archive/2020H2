import binascii
import socket


def parse_message(message):
    ID = message[0:4]
    PARAMS = message[4:8]
    QDCOUNT = message[8:12]
    ANCOUNT = message[12:16]
    NSCOUNT = message[16:20]
    ARCOUNT = message[20:24]

    first_domain_part_length = int(message[24:26], 16)
    first_domain_part = ""
    for i in range(first_domain_part_length):
        first_domain_part += chr(
            (int(message[26 + 2 * i:26 + 2 * (i + 1)], 16)))
    second_domain_part_begin = 26 + 2 * first_domain_part_length + 2

    second_domain_part_length = int(
        message[second_domain_part_begin - 2: second_domain_part_begin], 16)
    second_domain_part = ""
    for i in range(second_domain_part_length):
        second_domain_part += chr(
            int(message[second_domain_part_begin + 2 * i:second_domain_part_begin + 2 * (i + 1)], 16))

    domain = first_domain_part + "." + second_domain_part
    return domain


def find_record(domain):
    f = open("RR")
    for line in f:
        split_domain = line.split(" ")[0]
        IP = line.split(" ")[1]
        if (split_domain == domain):
            return IP
    return


def encode_url(url):
    first = url.split(".")[0]
    first_length = len(first)
    second = url.split(".")[1]
    second_length = len(second)
    return binascii.hexlify(first_length.to_bytes(1, byteorder='big')) \
        + binascii.hexlify(bytes(first, encoding='utf-8')) \
        + binascii.hexlify(second_length.to_bytes(1, byteorder='big')) \
        + binascii.hexlify(bytes(second, encoding='utf-8'))


def main():
    UDP_IP = "127.0.0.1"
    UDP_PORT = 5005

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    sock.bind((UDP_IP, UDP_PORT))

    while(True):
        data, addr = sock.recvfrom(2048)  # buffer size is 1024 bytes
        message = binascii.hexlify(data).decode("utf-8")
        print("recceived message:", message)
        parse_message(message)
        domain = parse_message(message)
        IP = find_record(domain)

        # header
        ID = "AA AA "
        PARAMS = "81 80 "
        QDCOUNT = "00 01 "
        ANCOUNT = "00 01 "
        NSCOUNT = "00 00 "
        ARCOUNT = "00 00 "
        HEADER = ID + PARAMS + QDCOUNT + ANCOUNT + NSCOUNT + ARCOUNT
        # question
        HOST = str(encode_url(domain))[1:].replace("'", "")
        QTYPE = "00 01 "
        QCLASS = "00 01 "
        QUESTION = HOST + QTYPE + QCLASS
        # answer
        NAME = "C0 0C "
        TYPE = "00 01"
        CLASS = "00 01"
        SOMETHING = "00 00"
        TTL = "18 4C"
        RDLENGTH = "00 04"
        split_IP = IP.split(".")
        IP = ""
        for i in split_IP:
            HEX = hex(int(i, 10))[2:]
            IP += "0" + HEX if (len(HEX) == 1) else HEX
        ANSWER = NAME + TYPE + CLASS + SOMETHING + TTL + RDLENGTH + IP
        RESULT = (HEADER + QUESTION + ANSWER).replace(
            " ", "").lower()
        sock.sendto(binascii.unhexlify(RESULT), addr)
        print(RESULT, "sent")


if __name__ == "__main__":
    main()
