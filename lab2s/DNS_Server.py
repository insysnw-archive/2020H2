import socket 
from DNS_Packet import DNS_Header, DNS_Answer

port = 53
ip = '0.0.0.0'
rr_lines=open("RR.txt","r").readlines()

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) #IPv4, UDP
sock.bind((ip, port))

def build_response(data):

    # unpack the request header
    request_header= DNS_Header()
    request_header.unpack(data[:12])

    # if we get qr=1, send header with rcode=1 or qdcount==0 
    # (server could not understand the request form)
    if request_header.qr!=0 or request_header.qdcount==0:

        return get_error_packet_with_rcode(data, 1)
        

    # if we get opcode!=0, send header with rcode=4 
    # (server cannot fulfill this type or request)
    if request_header.opcode!=0:

        return get_error_packet_with_rcode(data,4)


    # get the domain name from question section of a request
    curr=12
    name=b''
    while data[curr]!=0:
        amount=data[curr]
        for i in range(1,amount+1):
            curr+=1
            name+=data[curr].to_bytes(1,byteorder='big')
        name+=b'.'
        curr+=1


    res_ips=[]

    # try to find record in our local base (RR.txt)
    found=False
    for record in rr_lines:
        split=record.split(' : ')
        if split[0]==name.decode('utf-8'):
            res_ips.append(split[1])
            found=True

    # if there are no matching records, we just connect to 8.8.8.8, 
    # resend its responce to our client and save "domain : ip" to RR.txt 
    if not found:
        sock.sendto(data, ("8.8.8.8",53))
        google_resp, _ = sock.recvfrom(512)

        ind=12
        while google_resp[ind]!=0:
            ind+=google_resp[ind]+1
        ind+=5

        for k in range(google_resp[6]*256+google_resp[7]):
            if google_resp[ind]&0b10000000==128 and google_resp[ind]&0b01000000==64:
                ind+=12

            else:
                while google_resp[ind]!=0:
                    ind+=google_resp[ind]+1
                ind+=10

            res_ip=""
            for j in range(4):
                res_ip+=str(google_resp[ind])+'.'
                ind+=1
            res_ip=res_ip[:-1]+'\n'

            f=open("RR.txt","a")
            f.write(f"{name.decode('utf-8')} : {res_ip}")
            f.close()
            rr_lines.append(f"{name.decode('utf-8')} : {res_ip}")

        return google_resp

    # construct question section of a responce (just a copy of a request question section)
    responce_question_packed=data[12:curr+5]

    # construct answer section of a response
    responce_answers=[]
    for ip in res_ips:

        responce_answer=DNS_Answer()
        responce_answer.name=0b1100000000001100
        responce_answer._type=1
        responce_answer._class=1
        responce_answer.ttl=300

        res=b''
        for elem in ip.split('.'):
            res+=int(elem).to_bytes(1, byteorder='big')
        responce_answer.rdlength=len(res)
        responce_answer.rdata=res

        responce_answers.append(responce_answer)

    # construct the response header
    responce_header= DNS_Header()
    
    responce_header.id=request_header.id
    responce_header.qr=0b1
    responce_header.opcode= 0x0000
    responce_header.aa = 0b0
    responce_header.tc = 0b0
    responce_header.rd = request_header.rd
    responce_header.ra=0b1
    responce_header.rcode=0b0000
    responce_header.qdcount=0b0001
    responce_header.ancount=len(responce_answers)
    responce_header.nscount=0x0000
    responce_header.arcount=0x0000

    result_packed= responce_header.pack() + responce_question_packed

    for answer in responce_answers:
        result_packed+=answer.pack()

    return result_packed

def get_error_packet_with_rcode(data, rcode):

    request_header= DNS_Header()
    request_header.unpack(data[:12])

    responce_header= DNS_Header()

    responce_header.id=request_header.id
    responce_header.qr=0b1
    responce_header.opcode= 0x0000
    responce_header.aa = 0b0
    responce_header.tc = 0b0
    responce_header.rd = request_header.rd
    responce_header.ra=0b1
    responce_header.rcode=rcode
    responce_header.qdcount=0x0000
    responce_header.ancount=0x0000
    responce_header.nscount=0x0000
    responce_header.arcount=0x0000

    curr=12
    name=b''
    while data[curr]!=0:
        amount=data[curr]
        for i in range(1,amount+1):
            curr+=1
            name+=data[curr].to_bytes(1,byteorder='big')
        name+=b'.'
        curr+=1

    responce_question_packed=data[12:curr+5]

    return responce_header.pack() + responce_question_packed


while 1:
    data, addr = sock.recvfrom(512)
    r = build_response(data)
    sock.sendto(r, addr)
