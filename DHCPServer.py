from socket import *
import ipaddress
import sys

NETID = '192.168.1.0'
BROADCAST = '192.168.1.255'
serverPort = 12001
ipNet = ipaddress.ip_network('192.168.1.0/24')
clientList = {}
serverSocket = socket(AF_INET, SOCK_DGRAM)
serverSocket.bind(('', serverPort))
ipList = []
for ip in ipNet:
    ipList.append(str(ip))
ipList.remove(NETID)
ipList.remove(BROADCAST)

def sendMessage(outgoingMessage,cAdd):
    print('Sending message to client: %s' % outgoingMessage)
    serverSocket.sendto(outgoingMessage.encode(), cAdd)

def sendACK(msg_mac_addr, msg_IP, cAdd): 
    print('Server is sending Acknowledge message to client')
    message = 'ACK,' + msg_mac_addr + ',' + msg_IP
    sendMessage(message,cAdd)

def sendOFFER(msg_mac_addr, msg_IP, cAdd):
    print('Server is sending Offer message to client')
    clientIP = ipList.pop()      
    message = 'OFFER,' + msg_mac_addr +','+ clientIP
    sendMessage(message, cAdd)
            

def sendDECLINE(msg_mac_addr, msg_IP, cAdd):
    print('Server is sending Decline message to client')
    outmsg = 'DECLINE,' + msg_mac_addr + ', ' 
    sendMessage(outmsg, cAdd )

def checkIPs(clientIP):
    if clientIP in ipList:
        return True
    else:
        return False
  
#checks if the mac addr already has an IP 
def checkMACAddrs(msg_mac_addr):
    if msg_mac_addr in clientList:
        print('This MAC address already has an IP of ' + clientList.get(msg_mac_addr))
        return True
    else:
        print('This MAC address does not have an assigned IP yet')
        return False
   
def receiveDISCOVER(msg_mac_addr, msg_IP, cAdd): 
    print('Server received Discover message from client')
    if checkMACAddrs(msg_mac_addr) == False:
        if len(ipList) == 0:
            sendDECLINE(msg_mac_addr, msg_IP, cAdd)
        else:
            sendOFFER(msg_mac_addr, msg_IP, cAdd)
    else:
        print('Client is already assigned an IP address')
        sendACK(msg_mac_addr, msg_IP, cAdd)
     

def receiveRELEASE(msg_mac_addr, msg_IP, cAdd): 
    print('Server received Release message from client')
    try:
        if  msg_IP not in ipList:
            ipList.append(msg_IP)
        del clientList[msg_mac_addr]
    except KeyError:
        print("Key  not found")
    sendACK(msg_mac_addr, '0.0.0.0', cAdd)

def receiveRENEW(msg_mac_addr, msg_IP, cAdd): 
    try:
        if  msg_IP not in ipList:
            if msg_IP != '0.0.0.0':
                ipList.append(msg_IP)
                del clientList[msg_mac_addr]
    except KeyError:
        print("Key  not found")
    print('Server received Renew message from client')
    if checkMACAddrs(msg_mac_addr) == False:
        sendOFFER(msg_mac_addr, msg_IP, cAdd)


def receiveREQUEST(msg_mac_addr, msg_IP, cAdd):
    print('Server received a Request message from client')
    if msg_IP in clientList.values():
        if len(ipList) > 0:
            sendOFFER(msg_mac_addr, msg_IP, cAdd)
        else:
            sendDECLINE(msg_mac_addr, msg_IP, cAdd)
    else:
        clientList[msg_mac_addr] = msg_IP
        message = 'Client is assigned IP address:' + msg_IP
        print(message)
        sendACK(msg_mac_addr, msg_IP, cAdd)



def receiveList(msg_mac_addr, msg_IP, cAdd):
    print('Server received List request from admin')

    for mac in clientList:
        outmsg = 'LIST,' + mac + ',' + clientList.get(mac) + ','
        sendMessage(outmsg, cAdd)
    outmsg = 'END, , ,'
  


def receiveMessage(): 
    clientMessage, clientAddress = serverSocket.recvfrom(2048)
    clientMessage = clientMessage.decode()
    clientMessage = str(clientMessage)
    print(clientMessage)
    clientMessages = clientMessage.split(',')    
    msg_type = clientMessages[0]
    msg_mac_addr = clientMessages[1]
    msg_IP = clientMessages[2]
    if msg_type == 'DISCOVER':
        receiveDISCOVER(msg_mac_addr, msg_IP, clientAddress)
    elif msg_type == 'RELEASE':
        receiveRELEASE(msg_mac_addr, msg_IP, clientAddress)
    elif msg_type == 'RENEW':
        receiveRENEW(msg_mac_addr, msg_IP, clientAddress)
    elif msg_type == 'REQUEST':
        receiveREQUEST(msg_mac_addr, msg_IP, clientAddress)
    elif msg_type == 'LIST':
        receiveList(msg_mac_addr, msg_IP, clientAddress)
    else:
        print('Message not recognized!')
        sendDECLINE(msg_mac_addr, msg_IP, clientAddress)

def main():
   print('DHCP Server started')
   print('-------------------')
   while 1:
        receiveMessage()
        print()

main()
