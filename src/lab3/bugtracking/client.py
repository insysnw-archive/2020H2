import socket
import struct
import sys

ROLE = 0
USER_ID = 0


def safe_input(message: str) -> str:
    try:
        input_str = input(message)
    except KeyboardInterrupt:
        print("\nClient was closed! Goodbye :)")
        sys.exit(0)
    return input_str


def safe_input_int(message: str) -> str:
    try:
        input_str = input(message)

        while not isint(input_str):
            print('You can insert only unsigned integer!')
            input_str = safe_input(message)

    except KeyboardInterrupt:
        print("\nClient was closed! Goodbye :)")
        sys.exit(0)
    return input_str


host = ''
port = ''
try:
    host = sys.argv[1]
    port = sys.argv[2]
except IndexError:
    print('Usage: python3 client.py [ip] [port]')
    exit()

server = (host, int(port))

# connect to server
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(server)

action = safe_input('Register or authorize (reg / auth): ')

while action != 'reg' and action != 'auth':
    print('You can only choose reg or auth')
    action = safe_input('Register or authorize (reg / auth): ')


def isint(s):
    try:
        int(s)
        return True
    except ValueError:
        return False


if action == 'reg':
    user = safe_input_int('Your user_id (unsigned int): ')

    role = safe_input('Your role (dev / test): ')
    while role != 'dev' and role != 'test':
        print('You can only choose dev or test')
        role = safe_input('Your role (dev / test): ')

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
        ROLE = role_id
        USER_ID = user
    elif code == 102:
        receive = client.recv(4)
        user_id = struct.unpack('!I', receive)
        print(f'User_id {user_id[0]} is already exist. Please register again')
        exit()
    elif code == 1:
        print('Connection was closed by server')
        exit()


elif action == 'auth':
    user = safe_input_int('Your user_id: ')

    packet = struct.pack('!2I', 200, int(user))
    client.send(packet)

    receive = client.recv(4)
    code = struct.unpack('!I', receive)[0]

    if code == 201:
        receive = client.recv(4)
        role_id = struct.unpack('!I', receive)[0]

        if role_id == 0:
            role = 'dev'
        else:
            role = 'test'

        print(f'Success! You are authorize with user_id={user} as {role}')
        ROLE = role_id
        USER_ID = user
    elif code == 202:
        receive = client.recv(4)
        user_id = struct.unpack('!I', receive)
        print(f'User_id {user_id[0]} is not found. Please register!')
        exit()
    elif code == 1:
        print('Connection was closed by server')
        exit()


def get_bugs():
    status_id = safe_input('Status of bugs (0-opens, 1-resolved, 2-fixed): ')
    while status_id != '0' and status_id != '1' and status_id != '2':
        print('Status can be only 0 or 1 or 2')
        status_id = safe_input('Status of bugs (0-opens, 1-resolved, 2-fixed): ')

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


def add_bug():
    bug_id = safe_input_int('Bug id: ')
    project_id = safe_input_int('Project id: ')
    dev_id = safe_input_int('Developer id: ')
    s = bytes(safe_input('Description: '), 'ascii')

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


def fix_bug():
    bug_id = safe_input_int('Bug id: ')
    packet = struct.pack('!2I', 500, int(bug_id))
    client.send(packet)

    receive = client.recv(4)
    code = struct.unpack('!I', receive)[0]

    if code == 501:
        print(f'Bug {bug_id} was fixed by developers')
    elif code == 502:
        print(f'Bug {bug_id} is already closed')
    elif code == 503:
        print(f'No such bug with id {bug_id}')
    elif code == 3:
        print('Only developers can fix bugs, you are tester')
    elif code == 1:
        print('Connection was closed by server')
        exit()


def verify_bug():
    bug_id = safe_input_int('Bug id: ')

    resolution = safe_input('Resolution (0-open, 1-closed): ')
    while resolution != '0' and resolution != '1':
        print('Resolution can be only 1 or 0')
        resolution = safe_input('Resolution (0-open, 1-closed): ')

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


def quit():
    packet = struct.pack('!I', 700)
    client.send(packet)
    exit()


while True:
    if ROLE == 0:  # interface for devs
        action = safe_input('Command (get bugs / fix bug / quit): ')
        while action != 'get bugs' and action != 'fix bug' and action != 'quit':
            print('You can only choose listed commands')
            action = safe_input('Command (get bugs / fix bug / quit): ')

        if action == 'get bugs':
            get_bugs()

        elif action == 'fix bug':
            fix_bug()

        elif action == 'quit' or action == '^C':
            quit()

    elif ROLE == 1:  # interface for testers
        action = safe_input('Command (get bugs / add bug / verify bug / quit): ')
        while action != 'get bugs' and action != 'add bug' and action != 'verify bug' and action != 'quit':
            print('You can only choose listed commands')
            action = safe_input('Command (get bugs / add bug / verify bug / quit): ')

        if action == 'get bugs':
            get_bugs()

        elif action == 'add bug':
            add_bug()

        elif action == 'verify bug':
            verify_bug()

        elif action == 'quit' or action == '^C':
            quit()
