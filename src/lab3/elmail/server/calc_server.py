import select
import socket
import sys
import time


from src.lab3.elmail.protocol.socket_wrapper import (SocketWrapper)
from src.lab3.elmail.protocol.mail import (Mail)
from src.lab3.elmail.protocol.message import (Message)

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
                server_socket.setblocking(False)
                server_socket.bind((self.address, self.port))
            except OSError as e:
                print(f"Server close with error {str(e)}")
                sys.exit(1)

            # Listen to new connections
            server_socket.listen()
            # List of sockets for select.select()
            self.sockets.append(server_socket)

            print(f'Listening for connections on {self.address}:{self.port}...')

            while True:
                # Получаем живые сокеты
                read_sockets, _, exception_sockets = select.select(self.sockets, [], self.sockets)

                for skt in read_sockets:
                    socket_w = SocketWrapper(skt)
                    if skt == server_socket:
                        self.__handle_accept(socket_w)
                    else:
                        self.__handle_read(socket_w)

                # Отмываемся от мертвых сокетов
                for skt in exception_sockets:
                    self.sockets.remove(skt)

    # Обработка НОВОГО соединения
    def __handle_accept(self, server_socket: SocketWrapper):
        client_socket, client_address = server_socket.accept()
        self.sockets.append(client_socket)
        print('<{}>: Accepted new connection from {}:{}'.format(client_socket.__hash__(), *client_address))

    # Обработка запросов от СУЩЕСТВУЮЩИХ сокетов
    def __handle_read(self, socket_w: SocketWrapper):
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
            if message.msg_content["name"] in users_storage:
                err = "Such user is already logged in"
                socket_w.send(Message(-2, len(err), err))
            else:
                users_storage[socket_w.skt] = message.msg_content["name"]
                mail_storage[message.msg_content["name"]] = []
        elif message.msg_type == 1:
            if self.__validate_user(message, socket_w):
                if message.msg_content["to"] in mail_storage:
                    mail = Mail.from_dict(len(mail_storage[users_storage[socket_w.skt]]), users_storage[socket_w.skt],
                                          message.msg_content)
                    mail_storage[message.msg_content["to"]].append(mail)
                else:
                    err = "Invalid destination address"
                    socket_w.send(Message(-2, len(err), err))
        elif message.msg_type == 2:
            mails = mail_storage[users_storage[socket_w.skt]]
            filtered = list(filter(lambda x: not x is None, mails))
            socket_w.send(Message(2, len(filtered), filtered))
        elif message.msg_type == 3:
            mails = mail_storage[users_storage[socket_w.skt]]
            index = int(message.msg_content["index"])
            if index < len(mails):
                mail = mails[index]
                socket_w.send(Message(3, len(mail), mail))
            else:
                err = "Mail with such index does not exist"
                socket_w.send(Message(-2, len(err), err))
        elif message.msg_type == 4:
            mails = mail_storage[users_storage[socket_w.skt]]
            index = int(message.msg_content["index"])
            if index < len(mails):
                mails[index] = None
            else:
                err = "Mail with such index does not exist"
                socket_w.send(Message(-2, len(err), err))
        elif message.msg_type == 5:
            socket_w.close()

    def __validate_user(self, message, socket_w: SocketWrapper):
        if message.msg_content["from"] != users_storage[socket_w.skt]:
            err = "Invalid source address"
            socket_w.send(Message(-2, len(err), err))
            return False
        else:
            return True
