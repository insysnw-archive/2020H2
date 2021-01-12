import os
import socket
import struct
import sys
import threading

host = ''
port = ''
try:
    host = sys.argv[1]
    port = sys.argv[2]
except IndexError:
    print('Usage: python3 server.py [ip] [port]')
    exit()

serv = (host, int(port))

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind(serv)
server.listen()

clients = {}
users = {}
bugs = []


def in_bugs(bug_id):
    for bug in bugs:
        if bug_id == bug.id:
            return True


def find_bug(bug_id):
    for bug in bugs:
        if bug_id == bug.id:
            return bug


class Bug(object):
    def __init__(self, bug_id, project_id, status, test_id, dev_id, text):
        self.id = bug_id
        self.project = project_id
        self.status = status  # 0-open, 1-closed, 2-fix (QA)
        self.test = test_id
        self.dev = dev_id
        self.text = text


def process_client(client_sock, address):
    print(f'New client connected on {address}')

    try:
        while True:
            unpack = client_sock.recv(4)

            if unpack:
                code = struct.unpack('!I', unpack)[0]

                if code == 100:
                    register(client_sock)
                elif code == 200:
                    authorize(client_sock)
                elif code == 300:
                    buglist(client_sock)
                elif code == 400:
                    add_bug(client_sock)
                elif code == 500:
                    fix_bug_by_dev(client_sock)
                elif code == 600:
                    verify_bug_by_tester(client_sock)
                elif code == 700:
                    close_client(client_sock)
    except KeyboardInterrupt:
        print('Connection was closed by server')
        exit()


def register(sock):
    content = sock.recv(8)

    if content:
        unpacked = struct.unpack('!2I', content)
        user = unpacked[0]
        role = unpacked[1]

        if user in users.keys():
            packet = struct.pack('!2I', 102, user)
            sock.send(packet)
        else:
            users[user] = role
            packet = struct.pack('!I', 101)
            sock.send(packet)
            print(f"User with id={user} is registered and authorized with role={role}")
            clients[sock] = [user, role]


def authorize(sock):
    content = sock.recv(8)
    if content:
        unpacked = struct.unpack('!I', content)
        user = unpacked[0]

        if user not in users.keys():
            packet = struct.pack('!2I', 202, user)
            sock.send(packet)
        else:
            role = users[user]
            packet = struct.pack('!2I', 201, role)
            sock.send(packet)
            print(f"User with id={user} is authorized with role={role}")
            clients[sock] = [user, role]


def buglist(sock):
    unpacked = sock.recv(4)

    if unpacked:
        bug_status = struct.unpack('!I', unpacked)[0]

        if sock not in clients:
            packet = struct.pack('!I', 2)
            sock.send(packet)

        client_bugs = []

        for bug in bugs:
            if clients[sock][1] == 0:  # bugs for developer
                if bug.status == bug_status and bug.dev == clients[sock][0]:
                    client_bugs.append(bug)

            elif clients[sock][1] == 1:  # bugs for tester
                if bug.status == bug_status and bug.test == clients[sock][0]:
                    client_bugs.append(bug)

        if len(client_bugs) != 0:
            sock.send(struct.pack('!2I', 301, len(client_bugs)))

            for bug in client_bugs:
                description = f"bug_id={bug.id}, project_id={bug.project}, text: {bug.text}".encode('ascii')
                length = len(description).to_bytes(10, byteorder='big')
                sock.send(length + description)
        else:
            sock.send(struct.pack('!I', 302))


def add_bug(sock):
    unpacked = sock.recv(16)
    user = clients[sock][0]
    role = clients[sock][1]

    if unpacked:
        bug_info = struct.unpack('!4I', unpacked)

        bug_id = bug_info[0]
        dev_id = bug_info[1]
        project_id = bug_info[2]
        desc_length = bug_info[3]

        description = sock.recv(struct.calcsize(f'!{desc_length}s'))
        bug_text = struct.unpack(f'!{desc_length}s', description)[0].decode('ascii')

        if role == 0:
            sock.send(struct.pack('!I', 3))
            print('Only testers can add bug')
            return

        if in_bugs(bug_id):
            sock.send(struct.pack('!2I', 402, bug_id))
            return

        bugs.append(Bug(bug_id, project_id, 0, user, dev_id, bug_text))
        print(f'New open bug was added:\nbug_id={bug_id}, dev_id={dev_id}, test_id={user}, project_id={project_id}')

        sock.send(struct.pack('!I', 401))


def fix_bug_by_dev(sock):
    unpacked = sock.recv(4)
    role = clients[sock][1]

    if unpacked:
        bug_id = struct.unpack('!I', unpacked)[0]

        if role != 0:
            sock.send(struct.pack('!I', 3))
            return

        if not in_bugs(bug_id):
            sock.send(struct.pack('!I', 503))
        else:
            bug = find_bug(bug_id)

            if bug.status != 0:
                sock.send(struct.pack('!I', 502))
            else:
                bug.status = 2
                sock.send(struct.pack('!I', 501))
                print(f'Bug with id {bug_id} was fixed')


def verify_bug_by_tester(sock):
    unpacked = sock.recv(8)
    role = clients[sock][1]

    if role != 1:
        sock.send(struct.pack('!I', 3))
    else:
        if unpacked:
            bug_id = struct.unpack('!2I', unpacked)[0]
            resolution = struct.unpack('!2I', unpacked)[1]

            if not in_bugs(bug_id):
                sock.send(struct.pack('!I', 603))
            else:
                bug = find_bug(bug_id)

                if bug.status == 1:
                    sock.send(struct.pack('!I', 602))
                elif bug.status == 0:
                    sock.send(struct.pack('!I', 604))
                else:
                    if resolution != 0 and resolution != 1:
                        sock.send(struct.pack('!I', 605))
                    else:
                        bug.status = resolution
                        sock.send(struct.pack('!I', 601))
                        print(f'Bug with id {bug_id} was verified by tester, actual status is {bug.status}')


def close_client(sock):
    user = clients[sock][0]
    print(f"Connection from user {user} was closed")


while True:
    try:
        # thread for clients
        client_socket, address_socket = server.accept()
        thread = threading.Thread(target=process_client, args=(client_socket, address_socket))
        thread.daemon = True
        thread.start()
    except KeyboardInterrupt:
        print('\nConnection was closed by server')
        for socket in clients.keys():
            socket.send(struct.pack('!I', 1))
        exit()


