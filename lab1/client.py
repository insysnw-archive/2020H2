import socket
import datetime
import threading

address = (socket.gethostname(), 8686)
encoding = 'utf-8'


def getMessages():
	while True:
		data = s.recv(1024)
		print(data.decode(encoding))

if __name__ == '__main__':

	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

	s.connect(address)
	print("Your nickname:")
	nick = input()
	x = threading.Thread(target=getMessages, daemon=True)
	x.start()
	data = 'New user ' + nick
	s.sendall(data.encode(encoding))
	while True:
		message = input()
		data = "<" + datetime.datetime.now().strftime("%H:%M")+ "> [" + nick + "] " + message
		s.sendall(data.encode(encoding))
