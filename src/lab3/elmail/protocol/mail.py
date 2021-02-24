

class Mail:

    def __init__(self, id, source, destination, header, text):

        self.id = id
        self.source = source
        self.destination = destination
        self.header = header
        self.text = text

    @staticmethod
    def from_dict(id, name, content):
        return Mail(id, name, content["to"], content["header"], content["text"])
