import socket
import sys
import threading
import hashlib
import getpass
import re

HEADER_LENGTH = 5

RCODE = {
	'0' : "OK",
	'1' : "Dealer already exists",
	'2' : "Wrong password",
	'3' : "Such nickname already exists",
	'4' : "You have already placed a bet",
	'5' : "Not enough players",
	'6' : "Not enough money"
}

dealer = 0
player = 1
nick = ""
const_for_display = 5

if(len(sys.argv)!=3):
    print("USAGE: python chat_client.py [Server IP] [Server port]")
    sys.exit()

IP = sys.argv[1]
PORT = int(sys.argv[2])

balanse = 500

client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Request for registration
def create_registration_packet(role, msg):
    packet = bytearray()
    if role == dealer:
        packet.append(1<<7)
        packet += hashlib.sha512(msg.encode()).hexdigest().encode()
    else:
        packet.append(0)
        packet += (len(msg.encode()).to_bytes(HEADER_LENGTH,"big"))
        packet += msg.encode()
    return packet


def check_correct_answer(ans):
    code = str(ans[0]>>3 & 0x07)
    type_op, action = ans[0] >> 7, (ans[0] >> 6) & 0x01
    return {"rcode": code, "type_op": type_op, "action": action}


def registration():
    print("Welcome to Roulette")
    while True:

        role = input("Enter your role (dealer - D, player - P): ")
        if role == 'D':
            role = dealer
            msg = getpass.getpass(prompt='Enter password: ',stream=None)
        elif role == 'P':
            role = player
            msg = input("Enter your nickname: ")
            global nick
            nick = msg
        else:
            print("Wrong input")
            continue

        packet = create_registration_packet(role,msg)
        client_socket.send(packet)
        response = check_correct_answer(client_socket.recv(1))
        if response["rcode"] != '0':
            print("Response with error: {}".format(RCODE[response["rcode"]]))
            continue
        else:
            print("Success")
            game(role)
            break

# Request with new bet by player
def create_bet_packet(count,sums):
    packet = bytearray()
    packet.append(1<<5)
    packet += (count).to_bytes(1,"big")
    packet += (sums).to_bytes(2,"big")
    return packet

# Requst for start game by dealer
def create_start_game_packet(password):
    packet = bytearray()
    packet.append((1<<7)+(1<<5))
    packet += hashlib.sha512(password.encode()).hexdigest().encode()
    return packet

# Request for the current list of bets
def create_get_bets_packet():
    packet = bytearray()
    packet.append(2<<5)
    return packet


def display_list(response):
    global balanse

    if response["action"] == 1:
        print("RESULTS \n")

        # Acknoledgment
        packet = bytearray()
        packet.append(3<<5)
        client_socket.send(packet)

        result = int.from_bytes(client_socket.recv(1),"big")

        print("*"*(const_for_display+1) + "WIN  "+ str(result) + "*"*(const_for_display+1))
        if result % 2 == 0:
            print("*"*const_for_display + "EVEN NUMS" + "*"*const_for_display)
        else:
            print("*"*const_for_display + "ODD NUMS" + "*"*const_for_display)
        line = "NAME" + ' '*const_for_display*2 + "SUMM" + "\n"
    else:
        print("LIST OF CURRENT BETS \n")
        line = "NAME" + ' '*const_for_display*2 + "SUMM" + ' '*const_for_display*2 + "BET \n"
    
    print(line)
    new_balanse = ''
    count_list = int.from_bytes(client_socket.recv(HEADER_LENGTH),"big")

    for i in range(count_list):
        len_name = int.from_bytes(client_socket.recv(HEADER_LENGTH),"big")
        name = client_socket.recv(len_name).decode()
        summ = int.from_bytes(client_socket.recv(2),"big",signed=True)
        if response["action"] == 0:
            bet = int.from_bytes(client_socket.recv(1),"big",signed=True)
            print(name + ' '*const_for_display*2 + str(summ) + ' '*const_for_display*2 + str(bet) + '\n')
        else:
            print(name + ' '*const_for_display*2 + str(summ) + '\n')
        # New balanse
        if name == nick and response["action"] == 1:
            new_balanse = balanse + summ
        
    if response["action"] == 1:
        if new_balanse == '':
            print("You are not in the results \n")
            return
        elif balanse > new_balanse:
            print("You lose :( \n")
        else:
            print("Success! Congratulations! \n")
        balanse = new_balanse


def game(role):

    print("Ctrl+C to exit")
    try:
        while True:

            if role != dealer:
                bet = input("Place a bet: \n Enter sum (0-36, 37-place on even nums, 38-place on odd nums) \
and bet (your balance is {} for now) \n example: 2 200 \n Request for the current list of \
bets - press enter \n ".format(balanse))

                # Request for current bets
                if bet == '':
                    packet = create_get_bets_packet()
                # Request with new bet
                else:
                    ex = r'(\d+ \d+)'
                    correct_input = re.match(ex,bet)
                    if correct_input is not None and len(correct_input.group(0)) == len(bet):
                        count, sums = bet.split(" ")
                        if int(count) > 38 or int(sums) == 0:
                            print("Wrong input")
                            continue
                        packet = create_bet_packet(int(count),int(sums))
                    else:
                        print("Wrong input")
                        continue
                client_socket.send(packet)

                response = check_correct_answer(client_socket.recv(1))
                if response["type_op"] == 1:
                    display_list(response)
                elif response["rcode"] != '0':
                    print("Response with error: {}".format(RCODE[response["rcode"]]))
                else:
                    print("Success. Wait for results")
                    response = check_correct_answer(client_socket.recv(1))
                    if response["action"] == 1:
                        display_list(response)

            else:
                password = getpass.getpass(prompt='To start game enter password: ', stream=None)
                packet = create_start_game_packet(password)
                client_socket.send(packet)
                response = check_correct_answer(client_socket.recv(1))

                if response["rcode"] != '0':
                    print("Response with error: {}".format(RCODE[response["rcode"]]))
                else:
                    print("Success. Wait for results")
                    response = check_correct_answer(client_socket.recv(1))
                    display_list(response)

    except KeyboardInterrupt:
        client_socket.close()
        sys.exit() 


def main():

    client_socket.connect((IP, PORT))
    registration()


if __name__ == '__main__':
    main()