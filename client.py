import socket
import threading
import time

# choosing nickname
nickname = input("Nickname: ")
server = ('127.0.0.1', 55555)

# connect to server
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(server)


# listen to server
def receive():
    while True:
        try:
            # Receive Message From Server
            # If 'NICK' Send Nickname
            message = client.recv(1024).decode('ascii')
            if message == 'NICK':
                client.send(nickname.encode('ascii'))
            else:
                print(message)
        except OSError:
            break


# send to server
def write():
    while True:
        data = input('')
        if data == "#quit":
            client.send(data.encode('ascii'))
            client.close()
            break
        else:
            message = "<{}> [{}] {}".format(time.strftime('%H:%M', time.localtime()), nickname, data)
            client.send(message.encode('ascii'))


# start thread for listening and sending
receive_thread = threading.Thread(target=receive)
receive_thread.start()

write_thread = threading.Thread(target=write)
write_thread.start()
