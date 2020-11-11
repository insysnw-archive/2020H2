import socket
import time
import datetime
import threading


def local_time(time_of_message):
    timezone = -time.timezone / 3600
    time_of_message_format = datetime.datetime.strptime(time_of_message, "%H:%M")
    local_time_of_message = time_of_message_format + datetime.timedelta(hours=timezone)
    time_format = datetime.datetime.strftime(local_time_of_message, "%H:%M")
    return time_format


class ReceivingThread(threading.Thread):
    def __init__(self, server_socket):
        threading.Thread.__init__(self)
        self.ssocket = server_socket

    def run(self):
        while True:
            length_of_message = int(self.ssocket.recv(8).decode('UTF-8'))
            time_of_message = self.ssocket.recv(5).decode('UTF-8')
            length_name = int(self.ssocket.recv(8).decode('UTF-8'))
            name = self.ssocket.recv(length_name).decode('UTF-8')
            chunks = []
            bytes_record = 0
            while bytes_record < length_of_message:
                chunk = self.ssocket.recv(length_of_message)
                if chunk == b'':
                    raise RuntimeError("The socket connection is broken")
                chunks.append(chunk)
                bytes_record = bytes_record + len(chunk)
            data = b''.join(chunks)
            time_of_message = local_time(time_of_message)
            print("<" +time_of_message + "> "+ "[" + name + "] " + data.decode("UTF-8"))


SERVER = "127.0.0.1"
PORT = 7000
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect((SERVER, PORT))
login = input("Please, write your username: ")
client.send(bytes(login, 'UTF-8'))
print("Now you can start a conversation!")
thread_receive = ReceivingThread(client)
thread_receive.start()
while True:
    out_data = input()
    length = '{:08d}'.format(len(out_data))
    client.send(bytes(str(length), 'UTF-8'))
    client.send(bytes(datetime.datetime.utcnow().strftime("%H:%M"), 'UTF-8'))
    client.send(bytes(out_data, 'UTF-8'))
    if out_data == 'exit':
        break
client.shutdown(socket.SHUT_WR)
client.close()