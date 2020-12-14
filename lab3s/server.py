import socket
import select
import struct
import threading
import math
import multiprocessing 
import random
import time

operations = {
    '0': lambda x, y: x + y,
    '1': lambda x, y: x - y,
    '2': lambda x, y: x * y,
    '3': lambda x, y: x / y,
}

rcode = {
	'ok': 0,
	'time': 1,
	'math': 2,
	'id': 3,
	'wait': 4,
	'err': 5,
}

time_slow_op = 0.1 #sec

IP = "0.0.0.0"
PORT = 1234

wait_results = dict()

# Handles message receiving
# Parse socket id, type of operation, operation from 2 bytes
def receive_message(client_socket):

    # Receive 2 bytes main info
    try:
        message = client_socket.recv(2)
    except ConnectionResetError:
        return False
        
    # If we received no data, client gracefully closed a connection
    if not len(message):
        return False

    sock_id, type_op, op = ((message[0] << 5) + (message[1] >> 3), \
    	int(bin(message[1]>>2)[-1]), str(message[1] & 0x03))

    return {'id': sock_id, 'type': type_op, 'op': op}

# Get bytes with 1 or 2 arguments
size_of_arg = 4
def getArgs(client_socket,num,op):
    # Receive bytes with arguments
    try:
        message = client_socket.recv(num)
    except ConnectionResetError:
        return False
        
    # If we received no data, client gracefully closed a connection
    if not len(message):
        return False

    ans = []
    if op == 1:
    	_f = "!f f"
    else:
    	_f = "!f"
    ans = struct.unpack(_f,message)
    print(ans)

    # Return array with args
    return ans

# Create packet with result
def create_result_packet(sock_id, rcode, result,fraction=0):
    # 2 bytes with id and rcode, 16 bytes with result
    _format = "!H 2I"
    print("sock_id {}, rcode {}".format(sock_id,rcode))
    if result == 0:
    	_format = "!H"
    	ans = struct.pack(_format, (sock_id << 3) + rcode)
    	return ans
    if fraction == 0:
    	_format = "!H d"
    	ans = struct.pack(_format,(sock_id << 3) + rcode, result)
    	return ans
    ans = struct.pack(_format, (sock_id << 3) + rcode, int(result), fraction)
    return ans

# Calculate factorial with check time
def fact(n,ids,timeout):
    ans = 1
    for i in range(2,n+1):
        if (time.time() >= timeout):
            wait_results[str(ids)] = [rcode['time']]
            break
        else:
            ans = ans * i
    return ans

# Random id for client with slow operation
max_id = 8192
def get_random_id():
    ans_id = random.randint(1,max_id)
    while ans_id in wait_results.keys():
    	ans_id = random.randint(1,max_id)
    return ans_id

# Execution slow operation
result_32 = 2**32  #-1
def slow_op(sock,sock_id,op,n,timeout):
    time_out = time.time() + timeout
    try:
        if op == '0':
            result = fact(int(n),sock_id,time_out) 
            power = 0
            while result >= result_32:
                result //= 10
                power += 1
        else:
            power = 0
            result = math.sqrt(n)
            if (time.time() >= time_out):
                wait_results[str(sock_id)] = [rcode['time']]
        
        print("result {}".format(result))
        if wait_results[str(sock_id)][0] == rcode['wait']:
        	wait_results[str(sock_id)] = [rcode['ok'],op,result,power]
        print(wait_results)
  
    except:
        wait_results[str(sock_id)] = [rcode['math']]
    
def main():

	# Create a socket
	server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	server_socket.setblocking(0)

	server_socket.bind((IP, PORT))

	# Listen to new connections
	server_socket.listen()

	# List of sockets for select.select()
	sockets_list = [server_socket]

	print(f'Listening for connections on {IP}:{PORT}...')

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

	            # Add accepted socket to select.select() list
	            sockets_list.append(client_socket)

	            print('Accepted new connection from {}:{}'.format(*client_address))
	            

	        # Else handle request
	        else:

	            client_operation = receive_message(some_socket)
	            print(client_operation)

	            # If False, client disconnected
	            if client_operation is False:

	                print('Closed connection from: {}'.format(some_socket))

	                # Remove from list for socket.socket()
	                sockets_list.remove(some_socket)
	                continue

	            # Fast operation
	            if (client_operation['type'] == 0): 
	                args = getArgs(some_socket,size_of_arg*2,1)
	                try:
	                    result = operations[str(client_operation['op'])](args[0],args[1])
	                    print("result {}".format(result))
	                    packet = create_result_packet(client_operation['id'],rcode['ok'],result)
	                except Exception as e:
	                    print(str(e))
	                    packet = create_result_packet(client_operation['id'],rcode['math'],0)   
	                some_socket.send(packet) 

	            # Slow operation            
	            else: 
	            	# New request
	                if (client_operation['id'] == 0):
	                    args = getArgs(some_socket,size_of_arg,0)
	                    timeout = int.from_bytes(some_socket.recv(2),"big") / 1000

	                    if timeout == 0:
	                        timeout = time_slow_op
	                    print("timeout " + str(timeout))
	                    new_id = get_random_id()

	                    wait_results[str(new_id)] = [rcode['wait']]
	                    packet = create_result_packet(new_id,rcode['id'],0)
	                    some_socket.send(packet)

	                    threading.Thread(target=slow_op,args=(some_socket,new_id,str(client_operation['op']),args[0],timeout),daemon=True).start()
	                
	                # Get result
	                else:
	                    if (str(client_operation['id']) not in wait_results.keys()):
	                        packet = create_result_packet(client_operation['id'],rcode['err'],0)
	                    else:
	                        ans = wait_results[str(client_operation['id'])]
	                        if ans[0] == rcode['ok']:
	                            if ans[1] == '0':
	                                packet = create_result_packet(client_operation['id'],ans[0],ans[2],ans[3])
	                            else:
	                                packet = create_result_packet(client_operation['id'],ans[0],ans[2])
	                        else:
	                            packet = create_result_packet(client_operation['id'],ans[0],0)
	                        if ans[0] != rcode['wait']:
	                            del wait_results[str(client_operation['id'])]
	                    some_socket.send(packet)

	    # Handle some socket exceptions just in case
	    for some_socket in exception_sockets:

	        # Remove from list for socket.socket()
	        sockets_list.remove(some_socket)


if __name__=='__main__':
    main()