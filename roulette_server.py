import socket
import struct
import threading


from random import randint
# opcodes
OK = 0
BAD = 1
DUP_NAME = 2
DUP_DEALER = 1

TYPE_NAM = 0
TYPE_PLAY = 1

CODE_BET = 0
CODE_DEALER = 1
CODE_BALANCE = 2
CODE_LISTBET = 3

FORMAT = "utf-8"

# lists that will contain all the client sockets connected to the server and client's names.

clients = {}
curr_bets = {}

# send message to each client
def send_to_clients(message):
    for client in clients:
        client.send(message)


client_name = None
count = 0

# method to handle the incoming messages
def handle_client(connection, adress):
    global client_name, count
    while True:
        msg = connection.recv(1024)
        if len(msg) == 0:
            break
        typ = msg[0]
        if typ == 0:
            role = msg[1]
            nameb = msg[2:20]
            nam = nameb.decode('ascii')
            client_name = nam
            if role == 1:
                partition = 0
                users = clients.values()
                for u in users:
                    if u["name"] == nam:
                        partition = 1
                        break
                if partition == 0:
                    clients[connection] = {"name": nam, "role": 1, "money": None}
                    answ = bytes([TYPE_NAM]) + bytes([OK])
                    print(f"Крупье установлен")
                    connection.send(answ)
                else:
                    answ = bytes([TYPE_NAM]) + bytes([DUP_DEALER])
                    print(f"Крупье уже есть")
                    connection.send(answ)
            elif role == 0:
                partition = 0
                users = clients.values()
                for u in users:
                    if u["name"] == nam:
                        partition = 1
                        break
                if partition == 0:
                    clients[connection] = {"name": nam, "role": 0, "money": 100, "bet": None, "obj": None}
                    answ = bytes([TYPE_NAM]) + bytes([OK])
                    print(answ)
                    print(f"Новый игрок: {nam}")
                    connection.send(answ)
                else:
                    answ = bytes([TYPE_NAM]) + bytes([DUP_NAME])
                    print(answ)
                    connection.send(answ)
                    print(f"Дублирование имени")
            else:
                print(f"Неизвестный код")
                answ = bytes([TYPE_NAM]) + bytes([BAD])
                connection.send(answ)
        if typ == 1:
            print(msg)
            opcod = msg[1]
            if opcod == 0:
                bet = struct.unpack("h", msg[2:4])[0]
                betobj = msg[4]
                user = clients[connection]
                nm = user["name"]
                mon = user["money"]
                if mon < bet:
                    answ = bytes([TYPE_PLAY]) + bytes([0]) + bytes([1])
                    print(answ)
                    connection.send(answ)
                    print(f"Новая ставка: {bet} от {nm} на {betobj} не прошла ибо баланс не велик")
                else:
                    curr_bets[count] = {"name": nm, "bet": bet, "obj": betobj, "conn": connection}
                    user["money"] = user["money"] - bet
                    nm = user["name"]
                    answ = bytes([TYPE_PLAY]) + bytes([CODE_BET]) + bytes([OK])
                    print(answ)
                    connection.send(answ)
                    print(f"Новая ставка: {bet} от {nm} на {betobj} ")
                    count = count+1
            elif opcod == 2:
                user = clients[connection]
                nm = user["name"]
                balance = user["money"]
                print(f"Запрос баланса для: {nm} баланс: {balance}")
                answ = bytes([TYPE_PLAY]) + bytes([CODE_BALANCE]) + struct.pack('h', user["money"])
                print(answ)
                connection.send(answ)
            elif opcod == 1:
                print(f"Крупье начал")
                if len(clients) > 1 and len(curr_bets) > 0:
                    win = randint(0, 35)
                    print(f"Победило число: {win}")
                    all_bets = curr_bets.values()
                    for bt in all_bets:
                        obj = bt["obj"]
                        sum = bt["bet"]
                        us = bt["conn"]
                        user = clients[us]
                        if obj == win:
                            user["money"] += sum * 5
                        elif (win % 2 == 0 and obj == 36) or (win % 2 != 0 and obj == 37):
                            user["money"] += sum * 2

                    answ = bytes([TYPE_PLAY]) + bytes([CODE_DEALER]) + bytes([OK]) + bytes([win])
                    print(answ)
                    send_to_clients(answ)
                    curr_bets.clear()
                else:
                    answ = bytes([TYPE_PLAY]) + bytes([CODE_DEALER]) + bytes([BAD])
                    print(answ)
                    connection.send(answ)
            elif opcod == 3:
                print(f"Запрос ставок для {client_name}")
                for bet in curr_bets.values():
                    name = bet["name"]
                    bt = bet["bet"]
                    if bt is not None:
                        obj = bet["obj"]
                        print(f"Ставка: {bt} от {name} на {obj}")
                        n = name.encode('ascii')
                        b = struct.pack("h", bt)
                        o = obj
                        answ = bytes([TYPE_PLAY]) + bytes([CODE_LISTBET]) + b + bytes([o]) + n
                        print(answ)
                        connection.send(answ)
            else:
                print("some went wrong")
    try:
        connection.shutdown(socket.SHUT_RDWR)
        connection.close()
    except OSError:
        pass
    del clients[connection]
    ind = []
    count = 0
    for bets in curr_bets.values():
        if bets["name"] == client_name:
            ind.append(count)
            count += 1
    for i in ind:
        del curr_bets[i]
    print(client_name, 'disconnected')


server_ip = "0.0.0.0"
server_port = 1234

server_address = (server_ip, server_port)
print('Server is starting at', server_ip, ':', server_port)

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind(server_address)
server_socket.listen()

try:
    while True:
        # accept connection (returns a new socket to the client and the address bound to it)
        connection, address = server_socket.accept()  # connection == socket for client connection

        # start the handling thread
        client_thread = threading.Thread(target=handle_client, args=(connection, address))
        client_thread.start()

except KeyboardInterrupt:
    pass
finally:
    server_socket.shutdown(socket.SHUT_RDWR)
    server_socket.close()
    print("\nserver socket closed")
