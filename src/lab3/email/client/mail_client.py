import re
import socket
import sys

from src.lab3.email.protocol.socket_wrapper import SocketWrapper
from src.lab3.email.protocol import Message, Mail


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
                if len(client_args) == 2:
                    self.send_entry(client_args[1])
            elif client_args[0] == "send" and self.is_authed == 1:
                if len(client_args) == 2:
                    self.send_send(client_args[1])
            elif client_args[0] == "mailbox" and self.is_authed == 1:
                self.socket_w.send(Message(2, None))
            elif client_args[0] == "open" and self.is_authed == 1:
                self.send_command_about_mail(3, client_args[1])
            elif client_args[0] == "del" and self.is_authed == 1:
                self.send_command_about_mail(4, client_args[1])
            elif client_args[0] == "quit" and self.is_authed == 1:
                self.send_quit()
                break
            else:
                print("\nUnknown command")
                print()
                continue

            message = self.socket_w.recv()
            if message.msg_type == 0:
                print("Success.")
            elif message.msg_type == 2:
                mails = message.msg_content
                print("Mailbox")
                for x in mails:
                    self.print_mail(x)
                    print()

            elif message.msg_type == 3:
                mail = message.msg_content
                self.print_mail(mail)
            elif message.msg_type == -2:
                print(message.msg_content)
            else:
                print("\nUnknown packet")

    def send_entry(self, name):
        if re.match(r'[\w\.-]+@[\w\.-]+(\.[\w]+)+', name):
            msg = {"name": name}
            self.is_authed = 1
            self.socket_w.send(Message(0, msg))
        else:
            print("\nIncorrect mail format")

    def send_send(self, name):
        if re.match(r'[\w\.-]+@[\w\.-]+(\.[\w]+)+', name):
            msg = {"to": name}
            header = input("ВВедите заголовок письма: ")
            msg["header"] = header
            text = input("Введите текст письма: ")
            msg["text"] = text
            self.socket_w.send(Message(1, msg))
        else:
            print("\nIncorrect mail format")

    def send_command_about_mail(self, type, index):
        if re.match(r'^[0-9]+$', index):
            msg = {"index": index}
            if type == 3:
                self.socket_w.send(Message(3, msg))
            else:
                self.socket_w.send(Message(4, msg))
        else:
            print("\nIncorrect index format")

    def send_quit(self):
        self.is_authed = 0
        self.socket_w.send(Message(5, None))
        self.client_socket.close()
        print("Подключение разорвано")

    @staticmethod
    def print_mail(mail):
        for x in mail:
            print(x, ':', mail[x])
