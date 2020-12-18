import socket
import select
import hashlib
import time
import random
from _thread import start_new_thread

HEADER_LENGTH = 5

IP = "0.0.0.0"
PORT = 1234

MAX_HEADER_VALUE=pow(2,8*HEADER_LENGTH)

# Create a socket
# socket.AF_INET - address family, IPv4
# socket.SOCK_STREAM - TCP, conection-based
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Sets REUSEADDR (as a socket option) to 1 on socket
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

server_socket.bind((IP, PORT))

# Listen to new connections
server_socket.listen()

# List of sockets for select.select()
sockets_list = [server_socket]

# List of connected clients
clients = {}

dealer_socket = None


print(f'Listening for connections on {IP}:{PORT}...')

def parse_flags(client_socket):

    # Receive first byte (flags)
    try:
        flags = client_socket.recv(1)
    except ConnectionResetError:
        return None, None
    
    # If we received no data, client gracefully closed a connection
    if not len(flags):
        return None, None
    
    # Parse flags
    flags_int = int.from_bytes(flags, byteorder='big')
    flags_d = flags_int>>7
    flags_s = flags_int >>5 & 0b11

    return flags_d, flags_s


def player_registration(client_socket, flags_s):

    # New player registration
    if flags_s == 0:

        # Receive our "header" with name length
        header = client_socket.recv(5)

        name_length = int.from_bytes(header, byteorder='big')

        name=client_socket.recv(name_length).decode("utf-8")

        for key, value in clients.items():
            if value["name"]==name:
                print("Name is already exists")
                flags=0b00011000
                client_socket.send(flags.to_bytes(1, byteorder='big'))
                clients[client_socket] = {"name":None, "balance":500, "bet_choice":None, 
        "bet_amount":None, "accept":False}
                return

        # Also save username and username header
        clients[client_socket] = {"name":name, "balance":500, "bet_choice":None, 
        "bet_amount":None, "accept":False}

        flags=0b00000000
        client_socket.send(flags.to_bytes(1, byteorder='big'))

    else:
        print("Error: New connection with s!=0")



def dealer_registration(client_socket, flags_s):
    global dealer_socket
    # Dealer registration
    if flags_s == 0:

        if dealer_socket is not None:
            print("Dealer is already exists :(")
            flags=0b00001000
            client_socket.send(flags.to_bytes(1, byteorder='big'))
            _ = client_socket.recv(128).decode('utf-8')

        else:
            # Receive password
            dealer_password = client_socket.recv(128).decode('utf-8')

            stored_password=open("dealer_pass.txt",'r').readline()

            if dealer_password==stored_password:
                print("Dealer password is correct! Welcome, new dealer!")

                dealer_socket=client_socket

                flags=0b00000000
                client_socket.send(flags.to_bytes(1, byteorder='big'))
            else:
                print("Dealer password is wrong :(")
                flags=0b00010000
                client_socket.send(flags.to_bytes(1, byteorder='big'))

        clients[client_socket] = {"name":None, "balance":None, "bet_choice":None, 
        "bet_amount":None, "accept":False}

    else:
        print("Error: New connection with s!=0")

def handle_player_request(some_socket, flags_s):
    
    # Get user by socket
    user = clients[some_socket]

    if flags_s == 0:

        player_registration(some_socket, flags_s)

    elif flags_s == 1:

        request = some_socket.recv(3)

        # The client has already placed in this round, it is not possible to repeat the bet
        if user["bet_choice"] is not None:

            flags=0b01100000
            some_socket.send(flags.to_bytes(1, byteorder='big'))
            return

        bet_choice = request[0]
        bet_amount = int.from_bytes(request[1:], byteorder='big', signed= False)

        
        diff= user["balance"] - bet_amount

        if diff<0:
            print("Not enough money for bet")
            flags=0b01110000
            some_socket.send(flags.to_bytes(1, byteorder='big'))
            return

        user["bet_choice"] = bet_choice
        user["bet_amount"] = bet_amount

        flags=0b01000000
        some_socket.send(flags.to_bytes(1, byteorder='big'))

    elif flags_s == 2:

        flags=0b10000000
        
        active_bets=[]

        for key, value in clients.items():


            if value["bet_choice"] is not None and value["bet_amount"] is not None:

                active_bets.append(value)

        main_header=len(active_bets).to_bytes(HEADER_LENGTH, byteorder='big')

        resp=flags.to_bytes(1, byteorder='big') + main_header

        for elem in active_bets:

            enc_name = elem["name"].encode('utf-8') 

            resp+=(len(enc_name).to_bytes(HEADER_LENGTH, byteorder='big')+enc_name +
             elem["bet_amount"].to_bytes(2, byteorder='big')+
             elem["bet_choice"].to_bytes(1, byteorder='big'))


        some_socket.send(resp)

    elif flags_s == 3:

        user["accept"]=True

    else:

        print(f"Error: unexpected flags_s for player:{flags_s}")

def handle_dealer_request(some_socket, flags_s):

    global server_socket

    if flags_s == 0:

        dealer_registration(some_socket, flags_s)

    elif flags_s == 1:
        dealer_password = some_socket.recv(128).decode('utf-8')

        stored_password=open("dealer_pass.txt",'r').readline()
        if dealer_password==stored_password:

            game_res=random.randint(0,36)
            
            enc_game_res= game_res.to_bytes(1, byteorder='big')
            
            active_bets=[]

            for socket, value in clients.items():

                if value["bet_choice"] is not None and value["bet_amount"] is not None:

                    active_bets.append(value)

            if len(active_bets)<1:
                print("Not enough players")
                flags=0b01101000
                some_socket.send(flags.to_bytes(1, byteorder='big'))
                return

            # Send to dealer that everything is ok

            flags=0b01000000
            some_socket.send(flags.to_bytes(1, byteorder='big'))

            flags=0b11000000
            main_header=len(active_bets).to_bytes(HEADER_LENGTH, byteorder='big')

            resp=flags.to_bytes(1, byteorder='big') + enc_game_res+main_header

            for elem in active_bets:

                player_result = 0

                curr_choice=elem["bet_choice"]

                if curr_choice == game_res:
                    player_result = elem["bet_amount"] * 35
                elif (curr_choice == 37 and game_res%2==0)  or (curr_choice == 38 and game_res%2!=0):
                    player_result = elem["bet_amount"]
                else:
                    player_result = elem["bet_amount"] * -1

                elem["balance"]+=player_result

                elem["bet_choice"]=None
                elem["bet_amount"]=None
                enc_name = elem["name"].encode('utf-8') 

                resp+=(len(enc_name).to_bytes(HEADER_LENGTH, byteorder='big', signed= False)+enc_name +
                 player_result.to_bytes(2, byteorder='big', signed=True))
            
            start_new_thread(send_res,(resp, active_bets))
               
        else:

            print("Dealer password is wrong :(")
            flags=0b00010000
            some_socket.send(flags.to_bytes(1, byteorder='big'))

    else:

        print(f"Error: unexpected flags_s for dealer:{flags_s}")

def send_res(resp, active_bets):

    global server_socket

    active_bets.append(clients[dealer_socket])

    someone_send=False
    for i in range(4):
                
        for socket in sockets_list:

            if socket != server_socket and not clients[socket]["accept"] and clients[socket] in active_bets:
                        
                socket.send(resp)
                someone_send=True

        if not someone_send:
            break
        else:
            someone_send=False
        time.sleep(0.2) 

    for elem in active_bets:
        elem["accept"]=False

    clients[dealer_socket]["accept"]=False 


while True:

    # Calls Unix select() system call or Windows select() WinSock call with three parameters:
    #   - rlist - sockets to be monitored for incoming data
    #   - wlist - sockets for data to be send to
    #   - xlist - sockets to be monitored for exceptions (we want to monitor all sockets for errors)
    # Returns lists:
    #   - reading - sockets we received some data on (that way we don't have to check sockets manually)
    #   - writing - sockets ready for data to be send thru them
    #   - errors  - sockets with some exceptions
    read_sockets, _, exception_sockets = select.select(sockets_list, [], sockets_list)


    # Iterate over sockets
    for some_socket in read_sockets:

        # If socket is a server socket - new connection, accept it
        if some_socket == server_socket:

            # Accept new connection
            client_socket, client_address = server_socket.accept()

            flags_d, flags_s = parse_flags(client_socket)

            if flags_d is None:
                continue


            # D=0 -> request from a player
            if flags_d == 0:

                if len(clients)< MAX_HEADER_VALUE:
                    player_registration(client_socket, flags_s)
                    sockets_list.append(client_socket)
                else:

                    print("Error: Exceeded the player limit")

                
            # D=1 -> request from a dealer
            elif flags_d==1:
                dealer_registration(client_socket, flags_s)
                sockets_list.append(client_socket)

            else:
                print(f"Error, unexpected d: {flags_d}")

        # Else existing socket is sending a message
        else:

            flags_d, flags_s = parse_flags(some_socket)

            # Client or dealer disconnected
            if flags_d is None:

                user = clients[some_socket]

                if some_socket== dealer_socket:
                    dealer_socket=None
                    print("Dealer leaved")

                else:
                    print(f"Closed connection from: {clients[some_socket]['name']}")

                del clients[some_socket]
                # Remove from list for socket.socket()
                sockets_list.remove(some_socket)
                continue


            # D=0 -> request from a player
            if flags_d == 0:
                
                handle_player_request(some_socket, flags_s)
                
            # D=1 -> request from a dealer
            elif flags_d==1:

                handle_dealer_request(some_socket, flags_s)

            else:
                print(f"Error, unexpected d: {flags_d}")


    # Handle some socket exceptions just in case
    for some_socket in exception_sockets:

        # Remove from list for socket.socket()
        sockets_list.remove(some_socket)

        # Remove from our list of users
        del clients[some_socket]