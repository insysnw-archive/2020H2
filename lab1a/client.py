import socket
import datetime
import threading
import sys


if len(sys.argv) != 3:
    print("python3 client.py [Server IP] [Server port]")
    sys.exit()

IP = sys.argv[1]
PORT = int(sys.argv[2])
address = (IP,PORT)

header_length = 5

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

def getMessages():
	while True:
		try:
			data = s.recv(1)
			# Новый пользователь в чате
			if data[0] == 1:
				size_nick = int.from_bytes(s.recv(header_length),"big")
				data = s.recv(size_nick).decode()
				print("New user " + data)
			# Новое сообщение
			else:
				hours = int.from_bytes(s.recv(1),"big")
				minutes = int.from_bytes(s.recv(1),"big")
				size_nick = int.from_bytes(s.recv(header_length),"big")
				nick = s.recv(size_nick).decode()
				size_msg = int.from_bytes(s.recv(header_length),"big")
				msg = s.recv(size_msg).decode()
				print('<{0:02d}:{1:02d}> [{2}] {3}'.format(hours,minutes,nick,msg))
		except Exception as e:
			print("Exception: " + str(e))
			s.close()
			sys.exit()


def create_packet(msg,registration=0):
	ans = bytearray()
	# 0 - пакет с сообщением, 1 - пакет с nickом
	ans.append(registration)
	# 5 байт длина nickа/сообщения, далее сам текст
	ans += (len(msg.encode()).to_bytes(header_length,"big"))
	ans += msg.encode()
	return ans

if __name__ == '__main__':

	s.connect(address)

	print("Your nickname:")
	nick = input()
	packet = create_packet(nick,1)
	threading.Thread(target=getMessages, daemon=True).start()
	s.send(packet)
	try:
		# Отправка вводимых сообщений
		while True:
			message = input()
			packet = create_packet(message)
			s.send(packet)
	except:
		s.close()
		sys.exit()
