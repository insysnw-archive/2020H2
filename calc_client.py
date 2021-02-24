import socket
import sys
import re
import time
import threading
from _thread import start_new_thread
import struct

if (len(sys.argv) != 3):
    print("USAGE: python calc_client.py [Server IP] [Server port]")
    sys.exit()

IP = sys.argv[1]
PORT = int(sys.argv[2])
timeout = 10000

long_op_ids = []
run = True
long_op_check_event = threading.Event()

client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Connect to a given ip and port

client_socket.connect((IP, PORT))

# Set connection to non-blocking state
client_socket.setblocking(True)


def long_op_update():
    global long_op_ids
    global long_op_check_event

    while True:
        if run:
            time.sleep(1)
        long_op_check_event.set()


def main():
    # Create a socket
    # socket.AF_INET - address family, IPv4
    # socket.SOCK_STREAM - TCP, conection-based

    print("\nWelcome to our awesome calculator!")
    print("\nUsage:\nSimple operations: arg1 [+-*/] arg2\nNot so simple oprations: arg!, sqrt(arg)\n")
    start_new_thread(long_op_update, ())

    while True:
        global run
        global long_op_check_event
        run = True
        # Get input from client
        client_input = input().replace(" ", "")
        run = False
        long_op_check_event.wait()

        # check if input is a valid fast operation
        match_1 = re.match(r"-?(\d+\.)?\d+[\+\-\*\/]-?(\d+\.)?\d+", client_input)
        match_2 = re.match(r"\d+!", client_input)
        match_3 = re.match(r"sqrt\((\d+\.)?\d+\)", client_input)
        if match_1 and len(match_1.group()) == len(client_input):

            found_args = []
            found_span = []
            for el in re.finditer(r"-?(\d+\.)?\d+", client_input):
                found_args.append(el.group())
                found_span.append(el.span())

            client_input = (client_input[:found_span[0][1]] + ' ' +
                            client_input[found_span[0][1]:found_span[1][0]] + ' ' +
                            client_input[found_span[1][0]:])

            if (len(found_args) != 2):
                print(f"Something is wrong, got {len(found_args)} arguments insted of two")
                break

            op = client_input.split(" ")[1]

            if op == '+':
                id_flags = 0
            elif op == '-':
                id_flags = 1
            elif op == '*':
                id_flags = 2
            elif op == '/':
                id_flags = 3
            elif op == '':
                id_flags = 1
                found_args[1] = found_args[1][1:]
            else:
                print(f"Something went wrong, unknown operation: {op}")
                break
            try:
                if (id_flags == 3 and int(found_args[1]) == 0):
                    print("Деление на ноль")
                    continue
                else:
                    tp = 0
                    arg1 = float(found_args[0])
                    arg2 = float(found_args[1])
                    msg = bytes([tp]) + bytes([id_flags]) + struct.pack('f', arg1) + struct.pack('f', arg2)
                    client_socket.send(msg)
            except OverflowError:
                print('Too large number, i can\' count this :c')
                continue

            # get server response
            response = client_socket.recv(10)
            if not len(response):
                print('Connection closed by the server')
                client_socket.close()
                break
            typ = response[0]
            code = response[1]
            answ = struct.unpack("d", response[2:])
            # добавить проверку по коду
            if typ == 0:
                print(answ[0])
            elif typ == 2:
                print('Connection closed by the server')
                client_socket.close()
                break
            else:
                print(f"Unexpected RCODE from server: {typ}")

        # check if input is a valid slow operation
        elif (match_2 and len(match_2.group()) == len(client_input)) or (
                match_3 and len(match_3.group()) == len(client_input)):
            id_flags = 4
            op = client_input[-1]

            if op == '!':
                arg = float(client_input.split('!')[0])
                id_flags = 0
            elif op == ')':
                id_flags = 1
                arg = float(client_input.split('(')[1][:-1])
            else:
                print("Unexpected error during parsing, exiting")
                print(f'Op={op}')
                break

            try:
                tp = 1
                arg1 = float(arg)
                msg = bytes([tp]) + bytes([id_flags]) + struct.pack('f', arg1)
                client_socket.send(msg)
            except OverflowError:
                print('Too large number, i can\' count this :c')
                continue

            # get response from server
            response = client_socket.recv(15)
            if not len(response):
                print('Connection closed by the server')
                client_socket.close()
                break
            typ = response[0]
            codeop = response[2]
            codean = response[1]
            arr = response[3:7]
            arga = struct.unpack("f", arr)
            # добавить проверку по коду
            if codean == 0:
                answ = struct.unpack("d", response[7:15])
                if typ == 1:
                    if codeop == 1:
                        print(f"result of sqrt({arga[0]}) = {answ[0]} ")
                    else:
                        print(f"result of {arga[0]}! = {answ[0]} ")
                elif typ == 2:
                    print('Connection closed by the server')
                    client_socket.close()
                    break
                else:
                    print(f"Unexpected RCODE from server: {typ}")
            elif codean == 1:
                if codeop == 1:
                    print(f"Bad argument of sqrt({arga[0]})")
                else:
                    print(f"Bad argument of {arga[0]}!")
            if codean == 2:
                if codeop == 1:
                    print(f"Timer end of sqrt({arga[0]})")
                else:
                    print(f"Timer end of {arga[0]}!")

        # input is invalid, print usage
        elif client_input == "exit":
            print('Connection closed')
            client_socket.close()
            break
        else:
            print(
                "\nWrong input!\nUsage:\nSimple operations: arg1 [+-*/] arg2\nNot so simple oprations: arg!, "
                "sqrt(arg)\n in long operation arg! should be  positive and Integer. In operation sqrt(arg) arg "
                "should be positive! ")
            continue


if __name__ == '__main__':
    main()