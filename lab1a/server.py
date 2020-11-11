import socket
import threading

all_sockets = {}
data = ""
socket_from = socket
names = {}
length = 0
time = 0


def to_bytes(text):
    text_to_bytes = bytes(text, 'UTF-8')
    return text_to_bytes


def send_to_all(msg, sock):
    global all_sockets
    global names
    global socket_from
    global time
    for a in all_sockets.keys():
        if all_sockets[a] != sock:
            all_sockets[a].send(msg)
    return len(msg)


class ReceivingThread(threading.Thread):
    def __init__(self, client_address, client_socket):
        threading.Thread.__init__(self)
        self.csocket = client_socket
        self.cadrress = client_address

    def run(self):
        global all_sockets
        all_sockets[self.cadrress] = self.csocket
        global time
        global data
        global names
        global length
        global socket_from
        client_login = self.csocket.recv(2048).decode('UTF-8')
        names[self.csocket] = client_login
        while True:
            length = int(self.csocket.recv(8).decode('UTF-8'))
            time = self.csocket.recv(5).decode('UTF-8')
            print(time)
            socket_from = self.csocket
            chunks = []
            bytes_record = 0
            while bytes_record < length:
                chunk = self.csocket.recv(length)
                if chunk == b'':
                    raise RuntimeError("The socket connection is broken")
                if chunk == "exit":
                    break
                chunks.append(chunk)
                bytes_record = bytes_record + len(chunk)
            data = b''.join(chunks)
            print("Client:", data)


class SendingThread(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

    def run(self):
        global length
        while True:
            total_sent = 0
            global data
            global names
            global socket_from
            global time
            while total_sent < int(length):
                if data != '':
                    name_length = '{:08d}'.format(len(names[socket_from]))
                    length_name_message = '{:08d}'.format((int(length)))
                    send_to_all(to_bytes(length_name_message), socket_from)
                    send_to_all(to_bytes(time), socket_from)
                    send_to_all(to_bytes(name_length), socket_from)
                    send_to_all(to_bytes(names[socket_from]), socket_from)
                    sent = send_to_all(data[total_sent:], socket_from)
                    data = ""
                    if sent == 0:
                        raise RuntimeError("socket connection broken")
                    total_sent = total_sent + sent
                    print("send length"+length_name_message)


def main():
    localhost = "127.0.0.1"
    port = 7000
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((localhost, port))
    print("Server is running")
    server.listen(5)
    sending_thread = SendingThread()
    sending_thread.start()
    while True:
        client_sock, client_address = server.accept()
        thread_receive = ReceivingThread(client_address, client_sock)
        thread_receive.start()


if __name__ == '__main__':
    main()