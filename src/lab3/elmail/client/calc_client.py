import re
import socket
import sys
from time import sleep

from src.lab3.elmail.protocol.socket_wrapper import SocketWrapper
from src.lab3.elmail.protocol import Message, Mail


class Client:
    def __init__(self, address: str, port: int):
        self.address = address
        self.port = port
        self.client_socket = None
        self.socket_w: SocketWrapper = None
        self.is_authed = 0

    def start(self):
        try:
            self.__start()
        except KeyboardInterrupt:
            self.client_socket.close()
            print("\nКлиент успешно остановлен!")

    def __start(self):
        try:
            self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.client_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.client_socket.connect((self.address, self.port))
            self.client_socket.setblocking(True)
            self.socket_w = SocketWrapper(self.client_socket)
        except Exception as e:
            print(f"Не удалось подключиться к серверу. {str(e)}")
            sys.exit(1)

        print('''
Добро пожаловать в почтовый сервис! 
Поддерживаемые команды: 
entry "username" - подключение к серверу по логину 
send Адрес получателся - отправить письмо, после этой команды сервер запросит заголовок письма, а потом текст
mailbox - получение всех писем на вашем ящике
open "id" - открытие письма по идентификатору (узнать id писем можно по результату команды mailbox)
del "id" - удаление письма по идентификатору (узнать id писем можно по результату команды mailbox)
quit - принудительное отключение от сервера

        ''')

        while True:
            client_input = input("Введите команду: ")
            client_args = client_input.split(' ')

            if len(client_args) > 2:
                print("\nIncorrect command format")

            if client_args[0] == "entry":
                if re.match(r'[\w\.-]+@[\w\.-]+(\.[\w]+)+', client_args[1]):
                    msg = {"name": client_args[1]}
                    self.socket_w.send(Message(0, len(msg), msg))
                else:
                    print("\nIncorrect mail format")
            elif client_args[0] == "send":
                if re.match(r'[\w\.-]+@[\w\.-]+(\.[\w]+)+', client_args[1]):
                    msg = {"to": client_args[1]}
                    header = input("ВВедите заголовок письма: ")
                    msg["header"] = header
                    text = input("Введите текст письма: ")
                    msg["text"] = text
                    self.socket_w.send(Message(1, len(msg), msg))
                else:
                    print("\nIncorrect mail format")
            elif client_args[0] == "mailbox":
                self.socket_w.send(Message(2, None, None))
            elif client_args[0] == "open":
                if re.match(r'#^[0-9]+$#', client_args[1]):
                    msg = {"index": client_args[1]}
                    self.socket_w.send(Message(3, len(msg), msg))
                else:
                    print("\nIncorrect index format")
            elif client_args[0] == "del":
                if re.match(r'#^[0-9]+$#', client_args[1]):
                    msg = {"index": client_args[1]}
                    self.socket_w.send(Message(4, len(msg), msg))
                else:
                    print("\nIncorrect index format")
            elif client_args[0] == "quit":
                self.socket_w.send(Message(5, None, None))
            else:
                print("\nUnknown command")

            message = self.socket_w.recv()
            if message.msg_type == 2:
                mails = message.msg_content
                for x in mails:
                    print(x)
                    for y in mails[x]:
                        print(y, ':', mails[x][y])
            elif message.msg_type == 3:
                mail = message.msg_content
                for x in mail:
                    print(x, ':', mail[x])
            elif message.msg_type == -2:
                print(message.msg_content)
            else:
                print("\nUnknown packet")


    def __server_closed(self):
        self.client_socket.close()
        print("\nСоединение с сервером прервано!")
        sys.exit(1)
