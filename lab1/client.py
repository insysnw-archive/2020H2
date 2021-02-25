import socket
import sys
import time
import threading

if len(sys.argv) != 3:
    print("Specify IP and port")
    sys.exit()

address = (sys.argv[1], int(sys.argv[2]))
address = ('127.0.0.1', 5434)
mySocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
mySocket.connect(address)



def getPackage(s):
    header = s.recv(5)
    size = int.from_bytes(header, byteorder='big', signed=False)
    return s.recv(size).decode()


def getMessage():
    while True:
        try:
            nick = getPackage(mySocket)
            msg = getPackage(mySocket)
            print('<{}> [{}] {}'.format(time.strftime('%H:%M', time.localtime()), nick, msg))

        except Exception as e:
            print("Exception: " + str(e))
            mySocket.close()
            sys.exit()


def writeMessage():
    while True:
        msg = input()

        if msg:
            message = msg.encode()
            messageHeader = len(message).to_bytes(5, byteorder='big')
            mySocket.send(messageHeader + message)
            print('<{}> [{}] {}'.format(time.strftime('%H:%M', time.localtime()), nickname, msg))


if __name__ == '__main__':
    print(address)
    print("Your nickname")
    nickname = input().encode()
    nicknameHeader = len(nickname).to_bytes(5, byteorder='big')
    mySocket.send(nicknameHeader + nickname)
    getThread = threading.Thread(target=getMessage).start()
    writeThread = threading.Thread(target=writeMessage).start()
