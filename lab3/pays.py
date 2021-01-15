import shared

class RegisterRequest(shared.Packet):
    pid = 1

    def __init__(self, password=""):
        self.password = password

    def decode(self, decoder: shared.Decoder):
        self.password = decoder.read_string()

    def encode(self, encoder: shared.Encoder):
        encoder.write_string(self.password)

class RegisterResponse(shared.Packet):
    pid = 2

    def __init__(self, wallet=-1, money=-1):
        self.wallet = wallet
        self.money = money

    def decode(self, decoder: shared.Decoder):
        self.wallet = decoder.read_uint32()
        self.money = decoder.read_varint_big()

    def encode(self, encoder: shared.Encoder):
        encoder.write_uint32(self.wallet)
        encoder.write_varint_big(self.money)

class LoginRequest(shared.Packet):
    pid = 3

    def __init__(self, wallet=-1, password=""):
        self.wallet = wallet
        self.password = password

    def decode(self, decoder: shared.Decoder):
        self.wallet = decoder.read_uint32()
        self.password = decoder.read_string()

    def encode(self, encoder: shared.Encoder):
        encoder.write_uint32(self.wallet)    
        encoder.write_string(self.password)    

class LoginResponse(shared.Packet):
    pid = 4

    def __init__(self, money=-1):
        self.money = money

    def decode(self, decoder: shared.Decoder):
        self.money = decoder.read_varint_big()

    def encode(self, encoder: shared.Encoder):
        encoder.write_varint_big(self.money)

class WalletsRequest(shared.Packet):
    pid = 5

class WalletsResponse(shared.Packet):
    pid = 6

    def __init__(self, wallets=None, money=-1):
        if wallets == None:
            self.wallets = []
        else:
            self.wallets = wallets
        self.money = money

    def decode(self, decoder: shared.Decoder):
        count = decoder.read_varint()
        self.wallets = [decoder.read_uint32() for _ in range(count)]
        self.money = decoder.read_varint_big()

    def encode(self, encoder: shared.Encoder):
        encoder.write_varint(len(self.wallets))
        for wallet in self.wallets:
            encoder.write_uint32(wallet)
        encoder.write_varint_big(self.money)

class TransactionRequest(RegisterResponse):
    pid = 7

class TransactionResponse(shared.Packet):
    pid = 8

class Wallet:
    def __init__(self, id, password, money):
        self._id = id
        self._password = password
        self.money = money

    @property
    def id(self):
        return self._id

    @property
    def password(self):
        return self._password
