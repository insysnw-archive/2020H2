import socket
import argparse
import threading
import struct
import math
import multiprocessing as mp


# incoming message format: operation type, operation code, args
# outcoming message format: 
# for short: operation type, reply code, result
# for long: operation type, reply code, operation code, argument, result

# TYPES - short \ long operation type
SHT = 0  # short operation: sum, subtraction, multiplication, division
LNG = 1  # long operation: factorial, sqrt

# CODES - operation codes that work dependant on type
SUM = 0  # sum of 2 args if short type
FAC = 0  # factorial of 1 arg if long type
SUB = 1  # subtraction of 2 args if short type
SQT = 1  # square root of 1 arg if long type
MUL = 2  # multiplication of 2 args, short type
DIV = 3  # division of 2 args, short type

SHORT_OPERATIONS = {
    SUM: 'sum',
    SUB: 'subtraction',
    MUL: 'multiplication',
    DIV: 'division'
}

LONG_OPERATIONS = {
    FAC: 'factorial',
    SQT: 'square root'
}

# reply codes
RESULT_OK = 0
BAD_ARGUMENT = 1
SERVER_TIMEOUT = 2

MSG_SIZE = 15  # max message size according to protocol

FORMAT = "utf-8"

client_sockets = []

def evaluate_function(code, x):
    """Evaluate needed function."""
    if code == FAC:
        return math.factorial(x)
    else:
        return math.sqrt(x)

def handle_long_opareation(connection, message):
    """Handle long operation, then send result back to client."""
    arg = struct.unpack('f', message[2:6])[0]
    result = 0.0
    try:
        p = mp.Pool()  # create a pool of processes which will carry out tasks submitted to it
        res = p.apply_async(evaluate_function, (message[1], arg,))  # evaluate needed function asynchronously, runs in *only* one process
        result = res.get(timeout=30)  # server evaluates long operation for 30 seconds timeout. If calculations take more, exit and send client SERVER_TIMEOUT error code
    except ValueError as e:
        print('\noperation:', LONG_OPERATIONS[message[1]])
        print('arg:', arg)
        print(e)
        print(type(e))
        connection.send(bytes([LNG]) + bytes([BAD_ARGUMENT]) + message[1:6])
        return
    except mp.context.TimeoutError as e:
        print('\noperation:', LONG_OPERATIONS[message[1]])
        print('arg:', arg)
        print(e)
        print(type(e))
        connection.send(bytes([LNG]) + bytes([SERVER_TIMEOUT]) + message[1:6])
        return
    finally:
        p.close()

    try:
        reply = bytes([LNG]) + bytes([RESULT_OK]) + message[1:6] + struct.pack('d', float(result))
        connection.send(reply)
    except OverflowError as e:
        print(e)
        print(type(e))
        connection.send(bytes([LNG]) + bytes([BAD_ARGUMENT]) + message[1:6])

def handle_client(connection, connection_address):
    """Handle client connection."""
    while True:
        msg = connection.recv(MSG_SIZE)
        if len(msg) == 0:
            break

        elif msg[0] == SHT:
            result = 0.0
            arg1 = struct.unpack('f', msg[2:6])[0]
            arg2 = struct.unpack('f', msg[6:10])[0]

            if msg[1] == SUM:
                result = arg1 + arg2

            elif msg[1] == SUB:
                result = arg1 - arg2

            elif msg[1] == MUL:
                result = arg1 * arg2

            elif msg[1] == DIV:
                try:
                    result = arg1 / arg2
                except ZeroDivisionError as e:
                    print('\noperation:', SHORT_OPERATIONS[msg[1]])
                    print('args:', arg1, arg2)
                    print(e)
                    print(type(e))
                    connection.send(bytes([SHT]) + bytes([BAD_ARGUMENT]))
                    continue
            
            reply = bytes([SHT]) + bytes([RESULT_OK]) + struct.pack('d', result)
            connection.send(reply)

        elif msg[0] == LNG:
            calc_thread = threading.Thread(target=handle_long_opareation(connection, msg))
            calc_thread.start()

    try:
        calc_thread.join()
    except UnboundLocalError:  # maybe there isn't any calc_thread started
        pass
    connection.shutdown(socket.SHUT_RDWR)
    connection.close()
    client_sockets.remove(connection)
    print(connection_address, 'client disconnected')


# command line arguments parser
parser = argparse.ArgumentParser(description='Calculator server')
parser.add_argument("--ip", default='0.0.0.0', type=str, help='server IP address')
parser.add_argument("--port", default=1234, type=int, help='server port')
args = parser.parse_args()
server_address = (args.ip, args.port)
print('Server is starting at', server_address)


server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind(server_address)
server_socket.listen() 

try:
    while True:
        # accept connection (returns a new socket to the client and the address bound to it)
        connection, address = server_socket.accept()  # connection == socket for client connection
        client_sockets.append(connection)
        print(address, 'client connected')

        # start the handling thread
        client_thread = threading.Thread(target=handle_client, args=(connection, address))
        client_thread.start()

except KeyboardInterrupt:
    pass
finally:
    server_socket.shutdown(socket.SHUT_RDWR)
    server_socket.close()
    print("\nserver socket closed")
    
