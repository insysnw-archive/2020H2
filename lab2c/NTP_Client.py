import datetime
from NTP_Packet import NTP_Packet
import time
import socket

# Time difference between 1.1.1970 and 1.1.1900 (seconds)
format_diff = (datetime.date(1970, 1, 1) - datetime.date(1900, 1, 1)).days * 24 * 3600
# Waiting time for response (seconds)
waiting_time = 5
server = "pool.ntp.org"
port = 123

def get_time(additional_info=False):   
	packet = NTP_Packet(version=2, mode=3, transmit=time.time() + format_diff) # mode = 3 - client
	answer = NTP_Packet()
	with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s: # Use Ipv4, UDP
	    s.settimeout(waiting_time)
	    s.sendto(packet.pack(), (server, port))
	    data = s.recv(48)
	    arrive_time = time.time() + format_diff
	    answer.unpack(data)

	time_different = answer.receive_time-answer.originate_time-arrive_time+answer.transmit_time
	if not additional_info:
		return f'Time: {datetime.datetime.fromtimestamp(time.time() + time_different).strftime("%c")}'
	else:

		return "Time: {}\nTime difference: {}\n{}".format(
	    	datetime.datetime.fromtimestamp(time.time() + time_different).strftime("%c"),
	    	time_different,
	    	answer.to_display())
		
def main():
	print("Welcome to NTP client! Wanna get some time?"
		"\n1. Print time\n2. Print time with additional info\n3. Exit")
	while(True):
		user_input=input("\nCommand: ")
		if user_input == "1":
			print(get_time())
		elif user_input=="2":
			print(get_time(True))
		elif user_input=="3":
			exit()
		else:
			print("Sorry, I didn't understand :c")

if __name__=='__main__':
	main()