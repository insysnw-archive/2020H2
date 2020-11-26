import argparse
import os
from time import sleep

from errPackages import *

import socket
import struct
import sys


class Client:
    """
    Class Main that can get files from Server and put files to the server
    """

    def __init__(self, serverIP, serverPort, clientDir):
        self.serverIP = serverIP
        self.serverPort = serverPort
        self.clientDir = clientDir

        self.clientSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.clientSocket.settimeout(5)
        self._chunkSize = 512

    def get(self, fileName, localFileName=None):
        if localFileName == None:
            self.localFilePath = os.path.join(self.clientDir, fileName)
        else:
            self.localFilePath = os.path.join(self.clientDir, localFileName)

        self.fileName = fileName

        if os.path.isfile(self.localFilePath):
            print(self.localFilePath.split("/")[-1] + ' is alredy exist. Replacing.')
            os.remove(self.localFilePath)

        # --------------------- Send read request to server -------------------------
        ##Opcode 1 [ Read request ]
        ##
        ##          2 bytes    string   1 byte     string   1 byte
        ##          -----------------------------------------------
        ##   RRQ   |  01   |  Filename  |   0  |    Mode    |   0  |
        ##          -----------------------------------------------

        format = '!H' + str(len(self.fileName)) + 'sB5sB'
        self.sendPacket = struct.pack(format.encode(), 1, \
                                      self.fileName.encode(), 0, b'octet', 0)
        self.clientSocket.sendto(self.sendPacket, (self.serverIP, self.serverPort))

        try:
            getFile = open(self.localFilePath, 'wb')
        except:
            print(self.fileName + ' can not open.')
            return None

        totalDatalen = 0
        countBlock = 1
        errCount = 0

        while True:

            while errCount < 4:
                try:
                    data, remoteSocket = self.clientSocket.recvfrom(4096)
                    Opcode = struct.unpack('!H', data[0:2])[0]
                    errCount = 0
                    break
                except:
                    self.clientSocket.sendto(self.sendPacket, (self.serverIP, self.serverPort))
                    Opcode = 'Timeout'
                    errCount += 1
                    sleep(1)

            # --------------------- Get new block of file from server -------------------------
            ##Opcode 3 [ Data ]
            ##
            ##          2 bytes    2 bytes       n bytes
            ##          ---------------------------------
            ##   DATA  | 03    |   Block #  |    Data    |
            ##          ---------------------------------

            if Opcode == 3:

                blockNo = struct.unpack('!H', data[2:4])[0]
                if blockNo != countBlock:
                    self.clientSocket.sendto(errBlockNo, remoteSocket)
                    print('Receive wrong block. Continue')
                    # getFile.close()
                    continue

                countBlock += 1
                if countBlock == 65536:
                    countBlock = 1

                dataPayload = data[4:]

                try:
                    getFile.write(dataPayload)
                except:
                    self.clientSocket.sendto(errFileWrite, remoteSocket)
                    print('Can not write data. Session closed.')
                    getFile.close()
                    break

                totalDatalen += len(dataPayload)
                sys.stdout.write('\rget %s :%s bytes.' \
                                 % (self.fileName, totalDatalen))

                self.sendPacket = struct.pack(b'!2H', 4, blockNo)
                self.clientSocket.sendto(self.sendPacket, remoteSocket)

                if len(dataPayload) < self._chunkSize:
                    sys.stdout.write('\rget %s :%s bytes. finish.\n' \
                                     % (self.fileName, totalDatalen))
                    getFile.close()
                    break


            # --------------------- Error processing -------------------------
            elif Opcode == 5:

                errCode = struct.unpack('!H', data[2:4])[0]
                errString = data[4:-1]
                print('Received error code %s : %s' \
                      % (str(errCode), bytes.decode(errString)))
                getFile.close()
                os.remove(getFile.name)
                break


            elif Opcode == 'Timeout':
                print('Timeout. Session closed.')
                try:
                    getFile.close()
                except:
                    pass
                break


            else:

                print('Unknown error. Session closed.')
                try:
                    getFile.close()
                except:
                    pass
                break

    def put(self, fileName, targetFileName=None):
        self.localFilePath = os.path.join(self.clientDir, fileName)

        if targetFileName == None:
            self.fileName = fileName
        else:
            self.fileName = targetFileName

        if not os.path.isfile(self.localFilePath):
            print(self.fileName + ' not exist. Can not start.')
            return None

        # --------------------- Send write request to server -------------------------
        ##Opcode 2 [ Write request ]
        ##
        ##          2 bytes    string   1 byte     string   1 byte
        ##          -----------------------------------------------
        ##   WRQ   |  02   |  Filename  |   0  |    Mode    |   0  |
        ##          -----------------------------------------------

        format = '!H' + str(len(self.fileName)) + 'sB5sB'
        WRQpacket = struct.pack(format.encode(), 2, self.fileName.encode(), 0, b'octet', 0)
        self.clientSocket.sendto(WRQpacket, (self.serverIP, self.serverPort))

        try:
            putFile = open(self.localFilePath, 'rb')
        except:
            print(self.localFilePath + ' can not open.')
            return None

        endFlag = False
        totalDatalen = 0
        countBlock = 0

        while True:

            data, remoteSocket = self.clientSocket.recvfrom(4096)
            Opcode = struct.unpack('!H', data[0:2])[0]

            # --------------------- Send new block of file to server -------------------------
            ##Opcode 4 [ ack ]
            ##
            ##          2 bytes    2 bytes
            ##          --------------------
            ##   ACK   | 04    |   Block #  |
            ##          --------------------

            if Opcode == 4:

                if endFlag == True:
                    putFile.close()
                    sys.stdout.write('\rput %s :%s bytes. finish.\n' \
                                     % (self.fileName, totalDatalen))
                    break

                blockNo = struct.unpack('!H', data[2:4])[0]

                if blockNo != countBlock:
                    self.clientSocket.sendto(errBlockNo, remoteSocket)
                    print('Receive wrong block. Session closed.')
                    putFile.close()
                    break

                blockNo += 1
                if blockNo == 65536:
                    blockNo = 0

                dataChunk = putFile.read(self._chunkSize)

                DATApacket = struct.pack(b'!2H', 3, blockNo) + dataChunk
                self.clientSocket.sendto(DATApacket, remoteSocket)

                totalDatalen += len(dataChunk)
                sys.stdout.write('\rput %s :%s bytes.' \
                                 % (self.fileName, totalDatalen))

                countBlock += 1
                if countBlock == 65536:
                    countBlock = 0

                if len(dataChunk) < self._chunkSize:
                    endFlag = True


            # --------------------- Error processing -------------------------
            elif Opcode == 5:

                errCode = struct.unpack('!H', data[2:4])[0]
                errString = data[4:-1]
                print('Receive error code %s : %s' \
                      % (str(errCode), bytes.decode(errString)))
                putFile.close()
                break


            else:

                self.clear('Unknown error. Session closed.')
                try:
                    putFile.close()
                except:
                    pass
                break
               
               
def get_req_from_cmd(tftpClient):
    while True:
        command = input("\nINPUT FORMAT 'GET/PUT FILE_NAME' or 'EXIT'\n")
        command_list = command.split(" ")
        if command_list[0] == "EXIT" or command_list[0] == "exit":
            exit(0)
        elif len(command_list) < 2 or len(command_list) > 3:
            print("Unexpected arguments number")
            continue
        elif command_list[0] == "GET" or command_list[0] == "get":
            if len(command_list) == 2:
                return tftpClient.get, command_list[1], None
            else:
                return tftpClient.get, command_list[1], command_list[2]

        elif command_list[0] == "PUT" or command_list[0] == "put":
            if len(command_list) == 2:
                return tftpClient.put, command_list[1], None
            else:
                return tftpClient.put, command_list[1], command_list[2]

        else:
            print("Unexpected command '%s'" % command_list[0])


def main():
    # ---------------------------- parsing arguments ----------------------------
    parser = argparse.ArgumentParser(description="Some description")

    parser.add_argument("-p", "--port", type=int, action='store',
                        help="port")

    parser.add_argument("-i", "--ip", type=str, action='store',
                        help="ip")

    parser.add_argument("-d", "--dir", type=str, action='store',
                        help="direcotry with data")

    args = parser.parse_args()

    serverPort = args.port

    serverIp = args.ip

    clientDir = args.dir

    if serverPort is None:
        raise Exception("-p Port was't passed")
    if serverIp is None:
        raise Exception("-i IP was't passed")
    if clientDir is None:
        raise Exception("-d Dir was't passed")

    # ---------------------------- running client ----------------------------
    tftpClient = Client(serverIp, serverPort, os.path.abspath(clientDir))
    while True:
        request, fileName, targetFileName = get_req_from_cmd(tftpClient)

        request(fileName, targetFileName)


if __name__ == '__main__':
    main()