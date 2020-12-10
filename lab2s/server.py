import socket
import random
import os.path
import threading

tftp_ip = "0.0.0.0"
tftp_port = 69

tftp_opcodes = {
    1: 'RRQ',
    2: 'WRQ',
    3: 'DATA',
    4: 'ACK',
    5: 'ERROR'
}

tftp_modes = ['netascii', 'octet', 'mail']

tftp_errors = {
    0: 'Not defined',
    1: 'File not found',
    2: 'Access violation',
    3: 'Disk full or allocation exceeded',
    4: 'Illegal TFTP operation',
    5: 'Unknown transfer ID',
    6: 'File already exists',
    7: 'No such user'
}

connections = dict()
timeout = 5
max_repeat = 5 
current_dir = os.path.dirname(os.path.realpath(__file__))


def create_data_packet(block, filename, mode):
    data = bytearray()
    # Код операции (03)
    data.append(0)
    data.append(3)

    # Номер блока
    data += (block).to_bytes(2,"big")

    # Содержимое
    content = read_file(block, filename)
    data += content
    return data


def create_ack_packet(block):
    ack = bytearray()
    # Код операции (04)
    ack.append(0)
    ack.append(4)

    # Номер блока
    ack += (block).to_bytes(2,"big")

    return ack


def create_error_packet(error_code):
    err = bytearray()
    # Код операции (05)
    err.append(0)
    err.append(5)

    # Код ошибки
    err += (error_code).to_bytes(error_code,"big")

    # Сообщение
    msg = bytearray(tftp_errors[error_code].encode('utf-8'))
    err += msg
    err.append(0)

    return err


def send_packet(packet, socket, addr):
    socket.sendto(packet, addr)


def read_file(block, filename):
    with open(filename, 'rb') as f:
        offset = (block - 1) * 512
        f.seek(offset, 0) 
        content = f.read(512)
    return content


def create_udp_socket(ip=tftp_ip, port=tftp_port):
	sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) 
	sock.bind((ip,port))
	return sock


# Получаем код операции 
def get_opcode(bytes):
    opcode = int.from_bytes(bytes[0:2], byteorder='big')
    if opcode not in tftp_opcodes.keys():
        return False
    return tftp_opcodes[opcode]


# Получаем имя файла и режим передачи
def get_filename_mode(data):
    header = data[2:].split(b'\x00')
    filename = header[0].decode('utf-8');
    mode = header[1].decode('utf-8').lower()
    return filename, mode


def get_random_port():
    while True:
        port = random.randint(1025, 65536)
        if (port not in connections.keys()):
            return port


# Удаляем подключение
def close_connection(sock):
    port = sock.getsockname()[1]
    del connections[port]
    sock.close()


def listen(sock, filename, mode):
    (ip, port) = sock.getsockname()
    contact = connections[port]

    try:
        while True:
            try:
                sock.settimeout(timeout)
                data, addr = sock.recvfrom(1024)
                contact['timeouts'] = 0
                print(f'thread data: {data}')
                print(f'thread addr: {addr}')

                # Проверка адреса
                if addr != contact['addr']:
                    packet = create_error_packet(5)
                    send_packet(packet, socket, addr)
                    break

                opcode = get_opcode(data)
                if opcode == 'ACK':
                    block = int.from_bytes(data[2:4], byteorder='big')
                    # Проверка последнего ACK сообщения
                    if(len(contact['packet']) < 516 and block == int.from_bytes(contact['packet'][2:4], byteorder="big")):
                        break

                    packet = create_data_packet(block + 1, filename, mode)
                    contact['packet'] = packet
                    send_packet(packet, sock, addr)
                elif opcode == 'DATA':
                    block = int.from_bytes(data[2:4], byteorder='big')
                    content = data[4:]
                    with open(filename, 'ab+') as f:
                        f.write(content)

                    packet = create_ack_packet(block)
                    contact['packet'] = packet
                    send_packet(packet, sock, addr)

                    # Остановка после последнего ACK сообщения
                    if len(content) < 512:
                        print('closing connection')
                        break
                else:
                    # Ошибка Illegal operation
                    packet = create_error_packet(4)
                    send_packet(packet, sock, addr)
                    
            except socket.timeout:
                print(contact['timeouts'])
                if contact['timeouts'] < max_repeat:
                    contact['timeouts'] += 1
                    send_packet(contact['packet'], sock, contact['addr'])
                else:
                    break
        
        close_connection(sock)
        return False

    except Exception as e:
        print(e)
        close_connection(sock) 
        return False 


def main():

    sock = create_udp_socket()
    print("Server is running")

    while True:
        data, addr = sock.recvfrom(1024)
        print(f'data: {data}')
        print(f'addr: {addr}')

        opcode = get_opcode(data)

        if opcode == 'RRQ' or opcode == 'WRQ':
            filename, mode = get_filename_mode(data)

            # Проверка на ошибки
            if mode not in tftp_modes or mode == 'mail':
                packet = create_error_packet(0)
                send_packet(packet)
                continue

            if '/' in filename:
                packet = create_error_packet(2)
                send_packet(packet, sock, addr)
                continue

            if opcode == 'RRQ':
                # Если файл не существует
                if not os.path.isfile(f'{current_dir}/{filename}'):
                    packet = create_error_packet(1)
                    send_packet(packet, sock, addr)
                    continue

                packet = create_data_packet(1, filename, mode)
            else:
                
                if not os.path.isfile(f'{current_dir}/{filename}'):
                    with open(filename, 'w+'):
                        pass
                # Файл уже существует
                else:
                    packet = create_error_packet(6)
                    send_packet(packet, sock, addr)
                    continue

                packet = create_ack_packet(0)
            
            port = get_random_port()
            connections[port] = {
                'addr': addr, 
                'packet': packet,
                'timeouts': 0
            }

            client_socket = create_udp_socket(port=port)
            send_packet(packet, client_socket, addr)
            client = threading.Thread(target=listen, args=(client_socket, filename, mode),daemon=True)
            client.start()
            client.join()
        
        else:
            # Ошибка Illegal operation
            packet = create_error_packet(4)
            send_packet(packet, sock, addr)


if __name__ == '__main__':
    main()
