import socket
import sys
import threading
from time import sleep

from termcolor import colored

from src.lab3.calculator.client import utils
from src.lab3.calculator.client.socket_wrapper import SocketWrapper
from src.lab3.calculator.protocol import Operation, Response

class Client:
    def __init__(self, address: str, port: int):
        self.address = address
        self.port = port
        self.operation_count = 1
        self.client_socket = None
        self.socket_w: SocketWrapper = None
        self.last_input_msg = ''
        self.need_close = False

    def start(self):
        try:
            self.__start()
        except KeyboardInterrupt:
            print(colored("\nКлиент успешно остановлен!", 'green', attrs=['bold']))

    def __start(self):
        try:
            self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
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
Медленные операции выполняются только над ПОЛОЖИТЕЛЬНЫМИ числами
Для медленных операций через атрибут 't=' можно указать таймаут операции. Если он не указан, t=0.1
        ''')
        this = self
        self.pooling_thread = threading.Thread(target=Client.polling, args=(this,), daemon=True)
        self.pooling_thread.start()
        while True:
            client_input = self.input(f"[{self.operation_count}] Введите выражение: ")
            arg1, arg2, op_code = utils.parse(client_input)

            if self.__check_err(op_code):
                continue

            is_slow_operation = op_code > 3

            if is_slow_operation:
                operation = Operation(self.operation_count, op_code, arg1, timeout=arg2)
            else:
                operation = Operation(self.operation_count, op_code, arg1, arg2)

            self.socket_w.send(operation)

            print(f"[{self.operation_count}] Запрос успешно отправлен на сервер!")
            self.operation_count += 1

            if is_slow_operation:
                continue

            response = self.socket_w.recv()
            if response is None:
                self.__server_closed()

            self._check_rcode(response)

    def input(self, msg):
        self.last_input_msg = msg
        try:
            inp = input(msg)
        except:
            raise KeyboardInterrupt()
        return inp

    def __server_closed(self):
        if threading.main_thread() == threading.current_thread():
            print(colored('\nСоединение с сервером прервано!', 'red', attrs=['bold']))
            self.socket_w.close()
        else:
            print(
                colored('\nСоединение с сервером прервано! Введите любой символ чтобы выйти: ', 'red', attrs=['bold']),
                end='')
            self.need_close = True
        exit(1)

    # Кратковременный опрос на наличие пакетов
    def polling(self):
        while True:
            response = self.socket_w.recv(0.1)
            result = True
            if response is None:
                result = self.__server_closed()
            if result and response.operation_id > 0:
                msg = self.last_input_msg.replace(f'[{self.operation_count}]',
                                                  colored(f'[{self.operation_count}*]', None, attrs=['bold']))
                Client._check_rcode(response, start='\n', end=msg, color='red')

            sleep(0.2)

    def __check_err(self, answer):
        if not self.need_close:
            if answer is None:
                print("Переданно неверное выражение!")
                print("Попробуйте снова!")
                return True
            else:
                return False
        else:
            raise KeyboardInterrupt

    @staticmethod
    def _check_rcode(response: Response, start='', end='', color=None):
        if response.code == 0:
            msg = f"{start}[{response.operation_id}] Ответ: {response.result}"
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
