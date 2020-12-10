import binascii
import socket
import sys



def send_udp_message(message, address, port):
    #send_udp_message sends a message to UDP server
    #message should be a hexadecimal encoded string
    
    message = message.replace(" ", "").replace("\n", "")
    server_address = (address, port)

    my_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        my_socket.sendto(binascii.unhexlify(message), server_address)
        data, _ = my_socket.recvfrom(4096)
    finally:
        my_socket.close()
    return binascii.hexlify(data).decode("utf-8")


def encode_url(url):
    splitted_url=url.split(".")
    parts=b''
    for i in splitted_url:
        length=len(i)
        parts+= (binascii.hexlify(length.to_bytes(1, byteorder='big'))+ binascii.hexlify(bytes(i, encoding='utf-8')))
    return parts


def print_ip(res):
    ip = res[-8:]
    print(int(ip[0:2], 16), int(ip[2: 4], 16), int(
        ip[4: 6], 16), int(ip[6: 8], 16), sep='.')
    return


def print_answers_count(res):
    count = int(res[:16][-4:], 16)
    print("Answers recieved:", count)
    return


def main():
    ID = "AA AA "
    PARAMS = "01 00 "
    QUESTIONS_COUNT = "00 01 "
    ANSWERS_COUNT = "00 00 "
    NS_COUNT = "00 00 "
    OTHER = "00 00 "
    domain = sys.argv[1] if len(sys.argv) > 1 \
        and sys.argv[1] != None else "yandex.ru"
    ONLINE = True if (
        len(sys.argv) > 2 and sys.argv[2] != None and sys.argv[2] == "online") else False
    print("Looking for ip of", domain)

    message = ID + PARAMS + QUESTIONS_COUNT + ANSWERS_COUNT + NS_COUNT + OTHER \
        + encode_url(domain).decode("utf-8") + " 00 00 01 00 01"
    response = send_udp_message(message, "1.1.1.1", 53) if (ONLINE) \
        else send_udp_message(message, "127.0.0.1", 5005)

    print_answers_count(response)
    print_ip(response)


if __name__ == "__main__":
    main()