import select
import socket
import sys
import threading
import time

from src.lab3.calculator.protocol import (Response)
from . import utils
from .socket_wrapper import (SocketWrapper)

operations = {
    0: lambda a, b: utils.sum(a, b),
    1: lambda a, b: utils.sub(a, b),
    2: lambda a, b: utils.mul(a, b),
    3: lambda a, b: utils.div(a, b),
    4: lambda a, t: utils.sqrt(a, t),
    5: lambda a, t: utils.factorial(a, t),
}

rcode = {
    'ok': 0,
    'timeout': 1,
    'err': 2,
}


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
            print("\nServer stopped!")

    def __start(self):
        try:
            server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
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
        print('Accepted new connection from {}:{}'.format(*client_address))

    # Обработка запросов от СУЩЕСТВУЮЩИХ сокетов
    def __handle_read(self, socket_w: SocketWrapper):
        operation = socket_w.recv()

        if operation is None:
            print(f'Closed connection from: {socket_w.skt}')

            # Удаляем сокет из списка
            self.sockets.remove(socket_w.skt)
            del socket_w
            return None

        if operation.type < 4:
            try:
                code, result, msg = operations[operation.type](operation.operand1, operation.operand2)
                result = Response(operation.id, code, result, msg)
                socket_w.send(result)
            except Exception as e:
                print(str(e))
                socket_w.send(Response(operation.id, rcode['err'], -1, "Math error"))
        else:
            operation.timeout += time.time()
            threading.Thread(
                target=socket_w.send,
                args=(Response(operation.id, *operations[operation.type](operation.operand1, operation.timeout)),),
                daemon=True
            ).start()
