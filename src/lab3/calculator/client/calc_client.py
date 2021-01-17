import os
import socket
import sys
import threading
import signal
import time
from time import sleep

from termcolor import colored

from src.lab3.calculator.client import utils
from src.lab3.calculator.client.socket_wrapper import SocketWrapper
from src.lab3.calculator.protocol import Operation, Response


class Client:
    def __init__(self, address: str, port: int):
        self.address = address
        self.port = port
        self.operation_count = 0
        self.client_socket = None
        self.socket_w: SocketWrapper = None
        self.last_input_msg = ''
        self.result_recv = False
        self.recv_count = 0
        self.operations = {}

    def start(self):
        try:
            self.__start()
        except KeyboardInterrupt:
            self.client_socket.close()
            print(colored("\nКлиент успешно остановлен!", 'green', attrs=['bold']))

    def __start(self):
        try:
            self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.client_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.client_socket.connect((self.address, self.port))
            self.client_socket.setblocking(True)
            self.socket_w = SocketWrapper(self.client_socket)
        except Exception as e:
            print(colored(f"Не удалось подключиться к серверу. {str(e)}", 'red', attrs=['bold']))
            sys.exit(1)

        print('''
Добро пожаловать в консольный калькулятор! 
Поддерживаемые быстрые опреации: +-*/;
Поддерживаемые медленные операции: sqrt, !

Примеры медленных операций: 
1. sqrt(5)
2. sqrt(4.0) t=5
3. 4!
4. 5! t=6
        
Примечание: 
Медленные операции выполняются только над ПОЛОЖИТЕЛЬНЫМИ числами и результат приходит с ЗАДЕРЖКОЙ
Для медленных операций через атрибут 't=' можно указать таймаут операции. Если он не указан, t=0.1
        ''')
        this = self
        self.listen_thread = threading.Thread(target=Client.input_listener, args=(this,), daemon=True)
        self.listen_thread.start()
        while True:
            client_input = self.input(f"[{self.operation_count + 1}] Введите выражение: ")
            arg1, arg2, op_code = utils.parse(client_input)

            if self.__check_err(op_code):
                continue

            is_slow_operation = op_code > 3

            if is_slow_operation:
                operation = Operation(self.operation_count, op_code, arg1, timeout=arg2)
            else:
                operation = Operation(self.operation_count, op_code, arg1, arg2)

            self.socket_w.send(operation)
            self.operations[operation.id] = operation
            print(f"[{self.operation_count}] Запрос успешно отправлен на сервер!")

            if is_slow_operation:
                continue

            while not self.result_recv:
                sleep(0.1)
            self.result_recv = False

    def input(self, msg):
        self.last_input_msg = msg
        try:
            inp = input(msg)
            self.operation_count += 1
        except:
            raise KeyboardInterrupt()
        return inp

    def __server_closed(self):
        self.client_socket.close()
        time.sleep(2)
        print(colored('\nСоединение с сервером прервано!', 'red', attrs=['bold']))
        os.kill(os.getpid(), signal.SIGINT)
        sys.exit(1)

    def input_listener(self):
        while True:
            response = self.socket_w.recv(0.1)
            result = True
            if response is None:
                self.__server_closed()
            if result and response.operation_id > 0:
                self.recv_count += 1
                if response.operation_id == self.operation_count and self.operations[response.operation_id].type < 4:
                    self._check_rcode(response)
                    self.result_recv = True
                else:
                    all_oper_recv = self.recv_count == self.operation_count
                    msg = "" if not all_oper_recv else f"[{self.operation_count + 1}] Введите выражение: "
                    start = "" if not all_oper_recv else '\n'
                    self._check_rcode(response, start=start, end=msg, color='red')
                del self.operations[response.operation_id]
            sleep(0.05)

    # # Кратковременный опрос на наличие пакетов
    # def polling(self):

    def __check_err(self, answer):
        if answer is None:
            print("Переданно неверное выражение!")
            print("Попробуйте снова!")
            self.operation_count -= 1
            return True
        else:
            return False

    def _check_rcode(self, response: Response, start='', end='', color=None):
        operation = self.operations[response.operation_id]
        if response.code == 0 and operation.type < 4:
            msg = f"{start}[{response.operation_id}] Ответ: {response.result}"
        elif response.code == 0 and operation.type >= 4:
            answ_part0 = f"{start}[{response.operation_id}] "
            answ_part1 = f'{int(operation.operand1)}!' if operation.type == 5 else f'sqrt({operation.operand1})'
            answ_end = f' (t={operation.timeout})' if operation.timeout != 0 else ""
            answ_res = str(int(response.result)) if operation.type == 5 else str(response.result)
            msg = answ_part0 + answ_part1 + " = " + answ_res + answ_end
        elif response.code == 1:
            msg = f"{start}[{response.operation_id}] Время выполнения операции превысило заданный таймаут! \nПопробуйте повторить операцию, увеличив таймаут."
        elif response.code == 2:
            msg = f"{start}[{response.operation_id}] Ошибка выполнения операции: {response.msg}"
        else:
            msg = f"{start}[{response.operation_id}] Неизвестная ошибка"
        print(colored(msg, color))
        if end:
            print(f"{end}", end='')
            sys.stdout.flush()
