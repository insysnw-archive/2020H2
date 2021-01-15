import shared

class Post:
    def __init__(self, title, themes=None, content=None):
        if isinstance(title, shared.Decoder):
            decoder = title
            self.title = decoder.read_string()
            themes_count = decoder.read_varint()
            self.themes = [decoder.read_string() for _ in range(themes_count)]
            self.content = decoder.read_string()
        else:
            self.title = title
            self.themes = themes
            self.content = content
    
    def encode(self, encoder: shared.Encoder):
        encoder.write_string(self.title)
        encoder.write_varint(len(self.themes))
        for theme in self.themes:
            encoder.write_string(theme)
        encoder.write_string(self.content)

class GetAllThemesPacket(shared.Packet):
    pid = 1

class AllThemesPacket(shared.Packet):
    pid = 2

    def __init__(self, themes=None):
        if themes == None:
            self.themes = []
        else:
            self.themes = themes

    def decode(self, decoder: shared.Decoder):
        size = decoder.read_varint()
        self.themes = [decoder.read_string() for _ in range(size)]

    def encode(self, encoder: shared.Encoder):
        encoder.write_varint(len(self.themes))
        for theme in self.themes:
            encoder.write_string(theme)

class GetNewsPacket(shared.Packet):
    pid = 3

    def __init__(self, theme=""):
        self.theme = theme

    def decode(self, decoder: shared.Decoder):
        self.theme = decoder.read_string()

    def encode(self, encoder: shared.Encoder):
        encoder.write_string(self.theme)

class NewsPacket(shared.Packet):
    pid = 4

    def __init__(self, posts=None):
        if posts == None:
            self.posts = []
        else:
            self.posts = posts

    def decode(self, decoder: shared.Decoder):
        size = decoder.read_varint()
        self.posts = [(decoder.read_uint32(), decoder.read_string()) for _ in range(size)]

    def encode(self, encoder: shared.Encoder):
        encoder.write_varint(len(self.posts))
        for post in self.posts:
            encoder.write_uint32(post[0])
            encoder.write_string(post[1])

class GetPostPacket(shared.Packet):
    pid = 5

    def __init__(self, post_id=-1):
        self.post_id = post_id

    def decode(self, decoder: shared.Decoder):
        self.post_id = decoder.read_uint32()

    def encode(self, encoder: shared.Encoder):
        encoder.write_uint32(self.post_id)

class PostPacket(shared.Packet):
    pid = 6

    def __init__(self, post_id=-1, post=Post("", [], "")):
        self.post_id = post_id
        self.post = post

    def decode(self, decoder: shared.Decoder):
        self.post_id = decoder.read_uint32()
        self.post = Post(decoder)

    def encode(self, encoder: shared.Encoder):
        encoder.write_uint32(self.post_id)
        self.post.encode(encoder)

class AddPostPacket(shared.Packet):
    pid = 7

    def __init__(self, post=Post("", [], "")):
        self.post = post

    def decode(self, decoder: shared.Decoder):
        self.post = Post(decoder)

    def encode(self, encoder: shared.Encoder):
        self.post.encode(encoder)

class PostIdPacket(shared.Packet):
    pid = 8

    def __init__(self, post_id=-1):
        self.post_id = post_id

    def decode(self, decoder: shared.Decoder):
        self.post_id = decoder.read_uint32()

    def encode(self, encoder: shared.Encoder):
        encoder.write_uint32(self.post_id)
