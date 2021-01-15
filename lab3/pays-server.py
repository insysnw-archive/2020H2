#!/usr/bin/env python3

import argparse

parser = argparse.ArgumentParser(description='Pays server')
parser.add_argument('--host', default='localhost', help='server binding address')
parser.add_argument('--port', type=int, default=4242, help='server TCP port')
parser.add_argument('--money', type=int, default=1337, help='initial amount of money on a new wallet')
args = parser.parse_args()

import asyncio, logging, socket, shared, pays

logging.basicConfig(level=logging.INFO)

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind((args.host, args.port))

wallets_lock = asyncio.Lock()
wallet_by_id = {}
wallet_counter = 0

def walletsynch(function):
    async def decorated(*args, **kwargs):
        async with wallets_lock:
            return function(*args, **kwargs)
    return decorated

@walletsynch
def register(password: str) -> pays.Wallet:
    global wallet_counter
    wallet = pays.Wallet(wallet_counter, password, args.money)
    wallet_counter += 1
    wallet_by_id[wallet.id] = wallet
    return wallet

@walletsynch
def login(wallet_id: int, password: str) -> pays.Wallet:
    wallet = wallet_by_id.get(wallet_id, None)
    if wallet == None:
        raise shared.ClientError("no such wallet")
    if wallet.password != password:
        raise shared.ClientError("wrong password")
    return wallet

@walletsynch
def get_wallets() -> list[int]:
    return [wallet for wallet in wallet_by_id.keys()]

@walletsynch
def transaction(from_wallet: int, to_wallet: int, money: int):
    a = wallet_by_id.get(from_wallet, None)
    b = wallet_by_id.get(to_wallet, None)
    if a == None or b == None:
        raise shared.ClientError("no such wallet")
    if a.money < money:
        raise shared.ClientError("not enough money for transaction")
    a.money -= money
    b.money += money

class Connection:
    def __init__(self):
        self.wallet = None

    def authorised(self, foo):
        def decorated(packet: shared.Packet):
            if self.wallet == None:
                raise shared.ClientError("this action requires authorised access")
            else:
                return foo(packet)
        return decorated

async def handle_connection(reader, writer):
    try:
        address = writer.get_extra_info('peername')
        logging.info('%s connected', address)
        respondent = shared.Respondent(reader, writer)
        connection = Connection()

        @respondent.reply(pays.RegisterRequest)
        async def on_register(packet: pays.RegisterRequest) -> pays.RegisterResponse:
            wallet = await register(packet.password)
            connection.wallet = wallet
            return pays.RegisterResponse(wallet.id, wallet.money)

        @respondent.reply(pays.LoginRequest)
        async def on_login(packet: pays.LoginRequest) -> pays.LoginResponse:
            wallet = await login(packet.wallet, packet.password)
            connection.wallet = wallet
            return pays.LoginResponse(wallet.money)

        @respondent.reply(pays.WalletsRequest)
        async def on_get_wallets(packet: pays.WalletsRequest) -> pays.WalletsResponse:
            wallets = await get_wallets()
            money = 0
            if connection.wallet is not None:
                money = connection.wallet.money
            return pays.WalletsResponse(wallets, money)

        @respondent.reply(pays.TransactionRequest)
        @connection.authorised
        async def on_trnasaction(packet: pays.TransactionRequest) -> pays.TransactionResponse:
            await transaction(connection.wallet.id, packet.wallet, packet.money)
            return pays.TransactionResponse()

        await respondent.run()

    except Exception as ex:
        logging.error(ex)
    finally:
        logging.info('disconnected %s', address)

async def main():
    server = await asyncio.start_server(handle_connection, sock=server_socket)

    address = server_socket.getsockname()
    logging.info('binding to %s', address)

    async with server:
        await server.serve_forever()

try:
    asyncio.run(main())
except KeyboardInterrupt:
    pass
finally:
    server_socket.close()
    logging.info("socket closed")
