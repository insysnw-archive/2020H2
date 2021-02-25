import select
import socket
import sys
import threading
import time

from src.lab3.email.protocol.socket_wrapper import (SocketWrapper)
from src.lab3.email.protocol.mail import (Mail)
from src.lab3.email.protocol.message import (Message)

users_storage = {}
mail_storage = {}


class Server:
    def __init__(self, address: str, port: int):
        self.address = address
        self.port = port
        self.sockets = []
        self.exception_sockets = []

    def start(self):
        try:
            self.__start()
        except KeyboardInterrupt:
            for skt in self.sockets:
                skt.close()
            time.sleep(2)
            print("\nServer stopped!")

    def __start(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
            try:
                server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                server_socket.setblocking(True)
                server_socket.bind((self.address, self.port))
            except OSError as e:
                print(f"Server close with error {str(e)}")
                sys.exit(1)

            # Listen to new connections
            server_socket.listen()

            self.sockets.append(server_socket)

            print(f'Listening for connections on {self.address}:{self.port}...')

            threads = []

            while True:
                conn, addr = server_socket.accept()
                print("[-] Connected to " + addr[0] + ":" + str(addr[1]))
                socket_w = SocketWrapper(conn)
                thread = threading.Thread(target=Server.__handle_read, args=(self, socket_w,))
                thread.start()
                threads.append(thread)

    # Обработка запросов от СУЩЕСТВУЮЩИХ сокетов
    def __handle_read(self, socket_w: SocketWrapper):
        while True:
            message = socket_w.recv()
            if message.msg_type == -1:
                print(f'<{socket_w.skt.__hash__()}>: Closed connection')

                # Удаляем сокет из списка
                self.sockets.remove(socket_w.skt)
                del socket_w
                return None

            print(
                f"<{socket_w.skt.__hash__()}> [{message.msg_type}]: conteht={message.msg_content}"
            )
            if message.msg_type == 0:

                user = list(filter(lambda x: x[1] == message.msg_content["name"], users_storage.items()))
                print(user)
                if message.msg_content["name"] in users_storage.values():
                    err = "Such user is already logged in"
                    socket_w.send(Message(-2, err))
                else:
                    users_storage[socket_w.skt] = message.msg_content["name"]
                    mail_storage[message.msg_content["name"]] = []
                    print(users_storage)
                    print(mail_storage)
                    socket_w.send(Message(0, 'OK'))
            elif message.msg_type == 1:
                if message.msg_content["to"] in mail_storage:
                    print(users_storage[socket_w.skt])
                    mail = Mail.from_dict(len(mail_storage[users_storage[socket_w.skt]]), users_storage[socket_w.skt],
                                             message.msg_content)
                    mail_storage[message.msg_content["to"]].append(mail)
                    print("add")
                    print(mail_storage)
                    socket_w.send(Message(0, 'OK'))
                else:
                    err = "Invalid destination address"
                    socket_w.send(Message(-2, err))
            elif message.msg_type == 2:
                mails = mail_storage[users_storage[socket_w.skt]]
                filtered = list(filter(lambda x: not x is None, mails))
                mapped = list(map(lambda x: x.__dict__, filtered))
                socket_w.send(Message(2, mapped))
            elif message.msg_type == 3:
                mails = mail_storage[users_storage[socket_w.skt]]
                index = int(message.msg_content["index"])
                if index < len(mails) and mails[index] is not None:
                    mail = mails[index].__dict__
                    socket_w.send(Message(3, mail))
                else:
                    err = "Mail with such index does not exist"
                    socket_w.send(Message(-2, err))
            elif message.msg_type == 4:
                mails = mail_storage[users_storage[socket_w.skt]]
                index = int(message.msg_content["index"])
                if index < len(mails) and mails[index] is not None:
                    mails[index] = None
                    socket_w.send(Message(0, 'OK'))
                else:
                    err = "Mail with such index does not exist"
                    socket_w.send(Message(-2, err))
            elif message.msg_type == 5:
                del users_storage[socket_w.skt]
                socket_w.close()
                break

    def __validate_user(self, message, socket_w: SocketWrapper):
        if message.msg_content["from"] != users_storage[socket_w.skt]:
            err = "Invalid source address"
            socket_w.send(Message(-2, err))
            return False
        else:
            return True
