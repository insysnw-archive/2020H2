import socket
import select
import errno
import sys
import os
import re
import datetime
import time
import threading
from _thread import start_new_thread
import struct

if(len(sys.argv)!=3):
    print("USAGE: python calc_client.py [Server IP] [Server port]")
    sys.exit()

IP = sys.argv[1]
PORT = int(sys.argv[2])
timeout=10000

long_op_ids=[]
run = True
long_op_check_event=threading.Event()

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
            new_long_op_ids = []
            for i in range(len(long_op_ids)):

                curr=long_op_ids[i]

                enc_id_flags=curr[0]*8+4
                client_socket.send(enc_id_flags.to_bytes(2, byteorder='big', signed=False))

                responce=client_socket.recv(2)


                if not len(responce):
                    print('Connection closed by the server')
                    client_socket.close()
                    break

                rcode=int.from_bytes(responce[0:2],byteorder="big", signed=False) & 0b111

                if rcode == 0:
                    responce=client_socket.recv(8)

                    # If we received no data, server gracefully closed a connection
                    if not len(responce):
                        print('Connection closed by the server')
                        client_socket.close()
                        break

                    if curr[1]==0:

                        result_int=int.from_bytes(responce[0:8],byteorder="big", signed=False)

                        result_power=(int.from_bytes(responce[8:],byteorder="big", signed=False))

                        print(f"Long op result: {curr[2]}! = {result_int} * 10^{result_power}")

                    elif curr[1]==1:

                        unpacked_data=struct.unpack("!d", responce)
                        
                        result=unpacked_data[0]

                        print(f"Long op result: sqrt({curr[2]}) = {result}")

                    else:
                        print("Something went wrong, unexpected op in long_op_ckeck")
                        break

                elif rcode==1:

                    if curr[1] == 0:
                        print(f"Long op result: {curr[2]}! timeouted by server")

                    elif curr[1] == 1:
                        print(f"Long op result: sqrt({curr[2]}) timeouted by server")

                elif rcode == 2:

                    if curr[1] == 0:
                        print(f"Long op result: {curr[2]}! math error")

                    elif curr[1] == 1:
                        print(f"Long op result: sqrt({curr[2]}) math error")

                elif rcode == 4:
                    new_long_op_ids.append(curr)

                elif rcode == 5:

                    if curr[1] == 0:
                        print(f"Long op result: Can't find ID on server for {curr[2]}! ")

                    elif curr[1] == 1:
                        print(f"Long op result: Can't find ID on server for sqrt({curr[2]}) ")

                else:
                        print(f"Unexpected RCODE from server: {rcode}")
            
            long_op_ids=new_long_op_ids
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
        client_input = input().replace(" ","")
        run=False
        long_op_check_event.wait()
       
        # check if input is a valid fast operation
        match_1=re.match(r"-?(\d+\.)?\d+[\+\-\*\/]-?(\d+\.)?\d+",client_input)
        match_2=re.match(r"\d+!",client_input)
        match_3=re.match(r"sqrt\((\d+\.)?\d+\)",client_input)
        if match_1 and len(match_1.group())==len(client_input):

            found_args=[]
            found_span=[]
            for el in re.finditer(r"-?(\d+\.)?\d+",client_input):
                found_args.append(el.group())
                found_span.append(el.span())

            client_input=(client_input[:found_span[0][1]]+' '+
                client_input[found_span[0][1]:found_span[1][0]]+' '+
                 client_input[found_span[1][0]:])

            if(len(found_args)!=2):
                print(f"Something is wrong, got {len(found_args)} arguments insted of two")
                break

            op= client_input.split(" ")[1]
            
            if op=='+':
                id_flags=0
            elif op=='-': 
                id_flags=1
            elif op=='*': 
                id_flags=2
            elif op=='/': 
                id_flags=3
            elif op=='':
                id_flags=1
                found_args[1]=found_args[1][1:]
            else:
                print(f"Something went wrong, unknown operation: {op}")
                break

            try:
                client_socket.send(struct.pack("!H f f",id_flags, float(found_args[0]), float(found_args[1])))

            except OverflowError:
                print('Too large number, i can\' count this :c')
                continue

            # get server responce
            responce=client_socket.recv(10)

            # If we received no data, server gracefully closed a connection
            if not len(responce):
                print('Connection closed by the server')
                client_socket.close()
                break

            unpacked_data=struct.unpack("!H d", responce)

            rcode=unpacked_data[0] & 0b111

            if rcode == 0:

                result=unpacked_data[1]

                print(result)
            elif rcode == 1:
                print("Timeout on server during operation")
            elif rcode == 2:
                print("Math error on server during operation")
            else:
                print(f"Unexpected RCODE from server: {rcode}")

        # check if input is a valid slow operation
        elif (match_2 and len(match_2.group())==len(client_input)) or (match_3 and len(match_3.group())==len(client_input)) :

            id_flags=4
            op=client_input[-1]

            if op=='!':
                op=0
                arg=float(client_input.split('!')[0])
            elif op==')':
                op=1 
                id_flags+=1
                arg=float(client_input.split('(')[1][:-1])
            else:
                print("Unexpected error during parsing, exiting")
                print(f'Op={op}')
                break

            try:
                client_socket.send(struct.pack("!H f H",id_flags, arg, timeout))

            except OverflowError:
                print('Too large number, i can\' count this :c')
                continue

            # get responce from server
            responce=client_socket.recv(2)
            responce_int=int.from_bytes(responce[0:2],byteorder="big", signed=False)
            
            #parse rcode
            rcode=responce_int & 0b111
            if rcode !=3:
                print("Something went wrong, can't get ID from server")
                continue

            # parse id
            responce_id=responce_int >>3

            long_op_ids.append((responce_id, op, arg))

        # input is invalid, print usage
        else:
            print("\nWrong input!\nUsage:\nSimple operations: arg1 [+-*/] arg2\nNot so simple oprations: arg!, sqrt(arg)\n")
            continue

if __name__=='__main__':
    main()