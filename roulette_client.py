import socket
import argparse
import shlex
import sys
import threading
import struct


# packet types
LOG = 0  # initial login message
CMD = 1  # command message
DAT = 2  # data message

# user roles
PLAYER = 0
CROUPIER = 1

MSG_SIZE = 100  # max message size

FORMAT = 'ascii'  # message characters format

working = True


def handle_incoming():
    """Handle incoming messages."""
    global working
    while True:
        msg =  client_socket.recv(MSG_SIZE)

        # server disconnected
        if len(msg) == 0:
            working = False
            print('Server closed connection. If client is still running, press Enter.')
            break

        print('Server reply:', msg)

        # replies to client commands
        if msg[0] == 1:

            if msg[1] == 0:
                if msg[2] == 0:
                    print('Bet placed!')
                elif msg[2] == 1:
                    print('Not enough money.')

            # reply to croupier actions
            if msg[1] == 1:
                if msg[2] == 0:
                    print('Croupier has started the game!')
                    winner = msg[3]  # 1 byte winner target
                    print('Winner is', winner)
                elif msg[2] == 1:
                    print('Can`t start the game, not enough players or bets.')

            elif msg[1] == 2:
                balance = struct.unpack('H', msg[2:4])[0]
                print('Your balance:', balance)

            elif msg[1] == 3:
                i = 0
                while True:  # read msg
                    #print('i:',i , 'len(msg):', len(msg))
                    bet = struct.unpack('H', msg[i+2:i+4])[0]
                    if msg[i+4] == 36:
                        target = 'even'
                    elif msg[i+4] == 37:
                        target = 'odd'
                    else:
                        target = msg[i+4]
                    name_index = i + 5
                    name = ''
                    while name_index < len(msg):  # read name
                        if msg[name_index] == 1:
                            break
                        name += bytes([msg[name_index]]).decode(FORMAT)
                        name_index += 1
                    i = name_index
                    print('Name:', name, '\t\tTarget:', target, '\t\tSum:', bet)
                    if name_index >= len(msg) - 1:  # we have read last byte of msg
                        break

def login():
    """Login client."""
    while True:
        print('Enter your role and name separated by space. Roles are: player and croupier. Name is maximum 20 ASCII characters. Input example: player Jack')
        user = input().strip().split()
        print(user)
        if len(user) != 2 or (user[0] != 'player' and user[0] != 'croupier') or len(user[1]) > 20:
            print('Strange input, please try again.')
            continue
        else:
            return user


# command line arguments parser
parser = argparse.ArgumentParser(description='TFTP client')
parser.add_argument("--ip", default='127.0.0.1', type=str, help='server address')
parser.add_argument("--port", default=1234, type=int, help='server port')
args = parser.parse_args()
server_ip = args.ip
server_port = args.port
server_address = (args.ip, args.port)
print('connecting to', server_address)


# create a new client IPv4 TCP-socket
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# connect to the server
client_socket.connect(server_address)
print('Roulette client connected.')

# login phase
while True:
    user = login()
    role = PLAYER if user[0] == 'player' else CROUPIER
    name = user[1].encode(FORMAT)
    client_socket.send(bytes([LOG]) + bytes([role]) + name)
    server_reply = client_socket.recv(MSG_SIZE)
    if len(server_reply) == 0:
        print('Server disconnected, sorry. Try again later')
        sys.exit()
    elif server_reply[0] == 0 and server_reply[1] == 0:
        print('Connection established. Entering command mode. Type help for getting list of commands.')
        reading_thread = threading.Thread(target=handle_incoming)
        reading_thread.start()
        break
    elif server_reply[0] == 0 and server_reply[1] == 1:
        print('Croupier role has been picked already, please select player role.')
        continue
    elif server_reply[0] == 0 and server_reply[1] == 2:
        print('That name has been picked already, please choose another name.')
        continue
    else:
        print('Something very bad has happened, sorry.')
        sys.exit()

# playing phase
while True:
    if not working:
        break
    
    try:
        cmd, *args = shlex.split(input('> '))
    except ValueError:
        continue

    if cmd == 'exit':
        print('Ending connection with server.')
        client_socket.shutdown(socket.SHUT_RDWR)
        client_socket.close()
        break

    elif cmd == 'help':
        print('bet <target> <sum> - make a bet (player only). Target can be "even", "odd" or number from 0 to 35 including, sum - integer')
        print('start - start game (croupier only)')
        print('balance - get your current balance (player only)')
        print('bets - get all bets')
        print('exit - exit from roulette client')
        continue

    elif cmd == 'bet':
        #print(cmd, args)
        target = -1
        if not working:
            break
        if role != PLAYER:
            print('Bets are for players only!')
            continue
        if len(args) != 2:
            print('Strange bet format, see help and try again.')
            continue
        if args[0] != 'even' and args[0] != 'odd':
            try:
                target = int(args[0])
            except Exception as e:
                print(e)
                print(type(e))
                print('Strange bet format, see help and try again.')
                continue
            if target < 0 or target > 35:
                print('Target can be "odd", "even" or number between 0 and 35 including. Try again.')
                continue
        
        money = 0
        if args[0] == 'even':
            target = 36
        elif args[0] == 'odd':
            target = 37
        else:
            target = int(args[0])
        try:
            money = int(args[1])
        except Exception as e:
            print(e)
            print(type(e))
            print('Strange bet format, see help and try again.')
            continue
        msg = bytes([1, 0]) + struct.pack('H', money) + bytes([target])
        client_socket.send(msg)
        print('Client sent:', msg)
        continue

    elif cmd == 'start':
        if not working:
            break
        elif role != CROUPIER:
            print('Only croupier can start the game, please wait.')
            continue
        msg = bytes([1, 1])
        client_socket.send(msg)
        print('Client sent:', msg)
        continue

    elif cmd == 'balance':
        if not working:
            break
        elif role != PLAYER:
            print('Balance is for players only!')
            continue
        msg = bytes([1, 2])
        client_socket.send(msg)
        print('Client sent:', msg)
        continue

    elif cmd == 'bets':
        if not working:
            break
        msg = bytes([1, 3])
        client_socket.send(msg)
        print('Client sent:', msg)
        continue

    else:
        print('Unknown command: {}'.format(cmd))

reading_thread.join()
try:
    client_socket.shutdown(socket.SHUT_RDWR)
    client_socket.close()
except OSError:
    pass
