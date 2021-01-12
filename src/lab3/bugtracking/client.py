import socket
import struct
import sys

server = (int(sys.argv[1]), int(sys.argv[2]))

# connect to server
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(server)

action = input('Register of authorize (reg / auth): ')

while action != 'reg' and action != 'auth':
    print('You can only choose reg or auth')
    action = input('Register of authorize (reg / auth): ')


def isint(s):
    try:
        int(s)
        return True
    except ValueError:
        return False


if action == 'reg':
    user = input('Your user_id (unsigned int): ')

    while not isint(user):
        print('You can only choose unsigned integer user id!')
        user = input('Your user_id (unsigned int): ')

    role = input('Your role (dev / test): ')

    while role != 'dev' and role != 'test':
        print('You can only choose dev or test')
        role = input('Your role (dev / test): ')

    if role == 'dev':
        role_id = 0
    elif role == 'test':
        role_id = 1

    packet = struct.pack('!3I', 100, int(user), int(role_id))
    client.send(packet)

    try:
        receive = client.recv(4)
        code = struct.unpack('!I', receive)[0]
    except struct.error:
        print('Connection was closed by server')
        exit()

    if code == 101:
        print(f'Success! You are registered and authorize with user_id={user} as {role}')
    elif code == 102:
        receive = client.recv(4)
        user_id = struct.unpack('!I', receive)
        print(f'User_id {user_id[0]} is already exist. Please register again')
        exit()
    elif code == 1:
        print('Connection was closed by server')
        exit()


elif action == 'auth':
    user = input('Your user_id: ')

    while not isint(user):
        print('You can only choose unsigned integer user id!')
        user = input('Your user_id: ')

    packet = struct.pack('!2I', 200, int(user))
    client.send(packet)

    receive = client.recv(4)
    code = struct.unpack('!I', receive)[0]

    if code == 201:
        receive = client.recv(4)
        role_id = struct.unpack('!I', receive)
        if role_id == 0:
            role = 'dev'
        else:
            role = 'test'

        print(f'Success! You are authorize with user_id={user} as {role}')
    elif code == 202:
        receive = client.recv(4)
        user_id = struct.unpack('!I', receive)
        print(f'User_id {user_id[0]} is not found. Please register!')
    elif code == 1:
        print('Connection was closed by server')
        exit()


while True:
    action = input('Command (get bugs / add bug / fix bug / close bug / quit): ')

    while action != 'get bugs' and action != 'add bug' and action != 'fix bug' and action != 'close bug' and action != 'quit':
        print('You can only choose listed commands')
        action = input('Command (get bugs / add bug / fix bug / close bug / quit): ')

    if action == 'get bugs':
        status_id = input('Status of bugs (0-opens, 1-resolved): ')
        client.send(struct.pack('!2I', 300, int(status_id)))

        receive = client.recv(4)
        code = struct.unpack('!I', receive)[0]

        if code == 301:
            bug_num = struct.unpack('!I', client.recv(4))[0]
            print(f"{bug_num} bugs:")

            for i in range(0, bug_num):
                header = client.recv(10)
                length = int.from_bytes(header, byteorder='big', signed=False)
                description = client.recv(length).decode('ascii')

                print(f"[Bug {i + 1}] {description}")
        elif code == 302:
            print('There are no bugs with this status for you.')
        elif code == 2:
            print('You are not authorized!')
        elif code == 1:
            print('Connection was closed by server')
            exit()

    elif action == 'add bug':
        bug_id = input('Bug id: ')
        project_id = input('Project id: ')
        dev_id = input('Developer id: ')
        s = bytes(input('Description: '), 'ascii')

        packet = struct.pack(f'!5I{len(s)}s', 400, int(bug_id), int(project_id), int(dev_id), len(s), s)
        client.send(packet)

        receive = client.recv(4)
        code = struct.unpack('!I', receive)[0]

        if code == 401:
            print('New bug was added!')
        elif code == 402:
            b_id = struct.unpack('!I', client.recv(4))[0]
            print(f"Bug with id {b_id} is already in the system")
        elif code == 3:
            print('Only testers can add bug')
        elif code == 1:
            print('Connection was closed by server')
            exit()

    elif action == 'fix bug':
        bug_id = input('Bug id: ')
        packet = struct.pack('!2I', 500, int(bug_id))
        client.send(packet)

        receive = client.recv(4)
        code = struct.unpack('!I', receive)[0]

        if code == 501:
            print(f'Bug {bug_id} was fixed bu developers')
        elif code == 502:
            print(f'Bug {bug_id} is already closed')
        elif code == 503:
            print(f'No such bug with id {bug_id}')
        elif code == 3:
            print('Only developers can fix bugs, you are tester')
        elif code == 1:
            print('Connection was closed by server')
            exit()

    elif action == 'close bug':
        bug_id = input('Bug id: ')
        resolution = input('Resolution (0-open, 1-closed): ')
        packet = struct.pack('!3I', 600, int(bug_id), int(resolution))
        client.send(packet)

        receive = client.recv(4)
        code = struct.unpack('!I', receive)[0]

        if code == 601:
            print(f'Bug {bug_id} was verified by tester, actual status is {resolution}')
        elif code == 602:
            print(f'Bug {bug_id} is already closed')
        elif code == 603:
            print(f'No such bug with id {bug_id}')
        elif code == 604:
            print(f'Bug {bug_id} is not fixed yet')
        elif code == 605:
            print(f'Unknown resolution')
        elif code == 3:
            print('Only testers can close bug, you are developer')
        elif code == 1:
            print('Connection was closed by server')
            exit()

    elif action == 'quit' or action == '^C':
        packet = struct.pack('!I', 700)
        client.send(packet)
        exit()
