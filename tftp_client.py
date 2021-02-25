import socket
import os
from typing import Union
from pathlib import Path
from tftp_protocol import *


class TFTP_Client:
    def __init__(self,
                 server_ip: str = DEFAULT_IP,
                 server_port: int = DEFAULT_PORT,
                 root_dir: Union[str, Path] = DEFAULT_ROOT_DIR,
                 mode: str = DEFAULT_MODE):  
        self.server_ip = server_ip
        self.server_port = server_port
        self.root_dir = root_dir
        self.mode = mode
        self.addr = (server_ip, server_port)
        self.server_info = f'{server_ip}:{server_port}'
        self.server_sock = None

        self.client_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.client_sock.settimeout(CLIENT_TIMEOUT)

    def run_command(self, cmd, file_1, file_2):
        commands = {'get': self.get,
                    'put': self.put}
        if cmd not in commands.keys():
            return
        commands[cmd](file_1, file_2)

    def get(self, server_filename, client_filename):
        #process arguments
        file_path = os.path.join(self.root_dir, client_filename)
        if os.path.isfile(file_path):
            ans = self.replace_choice()
            if ans:
                print(f'Replacing {file_path}')
                os.remove(file_path)
            else:
                print(f'Exiting')
                return

        #RRQ to server
        current_packet = build_rrq_packet(server_filename, self.mode)
        self.client_sock.sendto(current_packet, self.addr)

        try:
            file_get = open(file_path, 'wb')
        except FileNotFoundError:
            print(f'{server_filename} can not open')
            return

        data_size = 0
        block_number = 1


        #Received packets handling
        while True:
            try:
                data, self.server_sock = self.client_sock.recvfrom(BUF_SIZE)
                opcode = data[0:2]
            except socket.timeout:
                self.client_sock.sendto(current_packet, self.addr)
                print('Timeout')
                file_get.close()
                os.remove(file_get.name)
                break

            if opcode == TFTP.OPCODES.DATA:
                #DATA packet from server
                #packet formatting
                current_block_number = struct.unpack('!H', data[2:4])[0]
                received_data = data[4:]

                #check block validation
                if current_block_number != block_number:
                    self.create_error(TFTP.ERROR_CODES.UNKNOWN_TRANSFER_ID)
                    continue
                block_number += 1
                if block_number == MAX_BLOCK_NUMBER:
                    block_number = 1

                try:
                    file_get.write(received_data)
                except FileNotFoundError:
                    self.create_error(TFTP.ERROR_CODES.FILE_NOT_FOUND)
                    file_get.close()
                    os.remove(file_get.name)
                    break

                data_size += len(received_data)
                print(f'\r[{self.server_info}] -- {server_filename} ({data_size} bytes)')

                #ACK packet to server
                current_packet = build_ack_packet(current_block_number)
                self.client_sock.sendto(current_packet, self.server_sock)

                if len(received_data) < CHUNK_SIZE:
                    print(f'\r[{self.server_info}] -- GET {server_filename} ({data_size} bytes) completed')
                    file_get.close()
                    break
            elif opcode == TFTP.OPCODES.ERROR:
                #Error packet from server
                #packet formatting
                err_code, err_msg = unpack_error(data)
                print(f'[{self.server_info}] -- ERROR: code({err_code}) msg({err_msg})')

                file_get.close()
                os.remove(file_get.name)
                break
            else:
                #unexpected packet from server
                self.create_error(TFTP.ERROR_CODES.ILLEGAL_OPERATION)
                file_get.close()
                break

    def put(self, client_filename, server_filename):
        root_path = os.path.join(self.root_dir, client_filename)

        if not os.path.isfile(root_path):
            print(server_filename + ' not exist. Can not start')
            return

        try:
            file_put = open(root_path, 'rb')
        except FileNotFoundError:
            print(f'{client_filename} can not open')
            return

        #WRQ to server
        current_packet = build_wrq_packet(server_filename, self.mode)  #filename or file_path
        self.client_sock.sendto(current_packet, self.addr)

        data_size = 0
        block_number = 0
        end_flag = False

        #Received packets handling
        while True:
            data, self.server_sock = self.client_sock.recvfrom(BUF_SIZE)
            opcode = data[0:2]

            if opcode == TFTP.OPCODES.ACK:
                #ACK packet from server
                if end_flag:
                    file_put.close()
                    print(f'\r[{self.server_info}] -- PUT {client_filename} ({data_size} bytes) completed.')
                    break

                current_block_number = struct.unpack('!H', data[2:4])[0]

                if current_block_number != block_number:
                    self.create_error(TFTP.ERROR_CODES.UNKNOWN_TRANSFER_ID)
                    continue

                current_block_number += 1
                if current_block_number == MAX_BLOCK_NUMBER:
                    current_block_number = 0

                data_chunk = file_put.read(CHUNK_SIZE)

                current_packet = build_data_packet(current_block_number, data_chunk)
                self.client_sock.sendto(current_packet, self.server_sock)

                data_size += len(data_chunk)
                block_number += 1
                if block_number == MAX_BLOCK_NUMBER:
                    block_number = 0
                if len(data_chunk) < CHUNK_SIZE:
                    end_flag = True
            elif opcode == TFTP.OPCODES.ERROR:
                #Error packet from server
                #packet formatting
                err_code, err_msg = unpack_error(data)
                print(f'[{self.server_info}] -- ERROR: code({err_code}) msg({err_msg})')
                file_put.close()
                break
            else:
                #unexpected packet from server
                self.create_error(TFTP.ERROR_CODES.ILLEGAL_OPERATION)
                file_put.close()
                break

    def create_error(self, error_code):
        error_package = build_error_packet(error_code)
        self.client_sock.sendto(error_package, self.server_sock)
        print(f'[{self.server_info}] -- {TFTP.ERROR_MSG[error_code]}')

    def replace_choice(self):
        ans = str(input('File already exists! Do you want to replace it? (y/n)')).lower()
        if ans not in ['y', 'n']:
            print('Invalid answer')
            return self.replace_choice()
        else:
            return ans == 'y'

    def __exit__(self, exc_type, exc_val, exc_tb):
        print('[Client] Stopping')
        self.client_sock.close()
        print('[Client] Stopped')


SERVER_IP = '127.0.0.1'
SERVER_PORT = 5004
ROOT_DIR = 'C:\Projects\\'
MODE = 'octet'


def print_separator():
    print('=' * 80)


def help_cmd():
    print('Available commands are GET and PUT\n'
          '\tGET command format: GET server_filename client_filename\n'
          '\tPUT command format: PUT client_filename server_filename')


def get_cmd():
    ans = str(input('Your command: ')).split(' ')
    cmd = ans[0].lower()
    if len(ans) != 3 or cmd not in ['get', 'put']:
        print('Invalid command')
        return get_cmd()
    else:
        return cmd, ans[1], ans[2]


if __name__ == '__main__':
    print('TFTP client is running')
    help_cmd()
    tftp_client = TFTP_Client(server_ip=SERVER_IP,
                              server_port=SERVER_PORT,
                              root_dir=os.path.abspath(ROOT_DIR),
                              mode=MODE)
    while True:
        print_separator()
        args = get_cmd()
        tftp_client.run_command(*args)