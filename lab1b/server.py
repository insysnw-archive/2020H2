import socket
import select

clients = {}

#controlling all length
def control_length(sock, length):
    chunk = b''
    try:
        chunk = sock.recv(length)
        clients[sock]['actual_length'] += len(chunk)
    except OSError:
        print('Received ', clients[sock]['actual_length'], 'out of ', length)
    return chunk

#receiving length of name from clients
def receiving_name_length(sock):
    clients[sock]['data_length'] += control_length(sock, 8)
    if clients[sock]['actual_length'] == 8:
        clients[sock]['actual_length'] = 0
        clients[sock]['data_length'] = int.from_bytes(clients[sock]['data_length'],byteorder='big',signed=False)
        clients[sock]['mode'] = 'name_mode'

#receiving name from clients
def receiving_name(sock):
    clients[sock]['data'] += control_length(sock, clients[sock]['data_length'])
    if clients[sock]['actual_length'] == clients[sock]['data_length']:
        clients[sock]['actual_length'] = 0
        clients[sock]['data_length'] = b''
        clients[sock]['name'] = str(clients[sock]['data'].decode('UTF-8'))
        clients[sock]['data'] = b''
        clients[sock]['mode'] = 'length_mode'

#receiving length of message from clients
def receiving_length(sock):
    clients[sock]['data'] += control_length(sock, 8)
    if clients[sock]['actual_length'] == 8:
        clients[sock]['actual_length'] = 0
        clients[sock]['length'].append(int(clients[sock]['data'].decode('UTF-8')))
        print(clients[sock]['length'][0])
        clients[sock]['data'] = b''
        clients[sock]['mode'] = 'time_mode'

#receiving time from clients
def receiving_time(sock):
    clients[sock]['data'] += control_length(sock, 5)
    if clients[sock]['actual_length'] == 5:
        clients[sock]['actual_length'] = 0
        clients[sock]['time'].append(str(clients[sock]['data'].decode('UTF-8')))
        print(clients[sock]['time'][0])
        clients[sock]['data'] = b''
        clients[sock]['mode'] = 'message_mode'

#receiving messages from clients
def receiving_message(sock):
    clients[sock]['data'] += control_length(sock, clients[sock]['length'][0])
    if clients[sock]['actual_length'] == clients[sock]['length'][0]:
        clients[sock]['actual_length'] = 0
        clients[sock]['message'].append(str(clients[sock]['data'].decode('UTF-8')))
        print(clients[sock]['message'][0])
        clients[sock]['data'] = b''
        clients[sock]['mode'] = 'length_mode'


def main():
    sockets_list = []
    localhost = "127.0.0.1"
    port = 5001
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((localhost, port))
    server.setblocking(False)
    server.listen()
    sockets_list.append(server)
    print(sockets_list)
    print(server.getblocking())
    print("Server is rinning")
    while True:
        r, w, e = select.select(sockets_list, [], [])
        for sock in r:
            if sock == server:
                #receiving all information from clients
                client_sock, client_address = server.accept()
                client_sock.setblocking(False)
                clients[client_sock] = {'name': None, 'message': [], 'time': [], 'length': [], 'mode': 'name_length',
                                        'actual_length': 0, 'data': b'', 'data_length': b''}
                sockets_list.append(client_sock)
            else:
                if clients[sock]['mode'] == 'name_length':
                    receiving_name_length(sock)
                elif clients[sock]['mode'] == 'name_mode':
                    receiving_name(sock)
                elif clients[sock]['mode'] == 'length_mode':
                    receiving_length(sock)
                elif clients[sock]['mode'] == 'time_mode':
                    receiving_time(sock)
                elif clients[sock]['mode'] == 'message_mode':
                    receiving_message(sock)
                if clients[sock]['message']:
                    #encryption for sending
                    message = bytes(clients[sock]['message'][0], 'UTF-8')
                    message_length = bytes(str('{:08d}'.format(clients[sock]['length'][0])), 'UTF-8')
                    time = bytes(clients[sock]['time'][0], 'UTF-8')
                    name = bytes(clients[sock]['name'], 'UTF-8')
                    length_name = bytes(str('{:08d}'.format(len(name))), 'UTF-8')
                    for client in clients.keys():
                        #sending information to all clients except for the person who sent the message
                        if client != sock:
                            client.send(length_name)
                            client.send(name)
                            client.send(time)
                            client.send(message_length)
                            client.send(message)
                    #reset variables
                    clients[sock]['message'].pop(0)
                    clients[sock]['time'].pop(0)
                    clients[sock]['length'].pop(0)
                    #change mode
                    clients[sock]['mode'] = 'length_mode'


if __name__ == '__main__':
    main()