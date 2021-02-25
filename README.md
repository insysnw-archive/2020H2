 ### Инструкция по использованию
 Запустить скрипт командой `python3 DHCPClient.py`. Для использования порта 67 необходимо запускать программу с правами суперпользователя: `sudo python3 DHCPServer.py`.
 
 ### Описание используемого протокола.
 Структура DHCP-сообщения, в скобках указана длина поля в октетах (байтах):
 
 ```
    0               1               2               3
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |     op (1)    |   htype (1)   |   hlen (1)    |   hops (1)    |
    +---------------+---------------+---------------+---------------+
    |                            xid (4)                            |
    +-------------------------------+-------------------------------+
    |           secs (2)            |           flags (2)           |
    +-------------------------------+-------------------------------+
    |                          ciaddr  (4)                          |
    +---------------------------------------------------------------+
    |                          yiaddr  (4)                          |
    +---------------------------------------------------------------+
    |                          siaddr  (4)                          |
    +---------------------------------------------------------------+
    |                          giaddr  (4)                          |
    +---------------------------------------------------------------+
    |                                                               |
    |                          chaddr  (16)                         |
    |                                                               |
    |                                                               |
    +---------------------------------------------------------------+
    |                                                               |
    |                          sname   (64)                         |
    +---------------------------------------------------------------+
    |                                                               |
    |                          file    (128)                        |
    +---------------------------------------------------------------+
    |                                                               |
    |                          options (variable)                   |
    +---------------------------------------------------------------+
 ```
 
 ```
    FIELD      OCTETS       DESCRIPTION
    -----      ------       -----------
 
    op            1  Message op code / message type.
                     1 = BOOTREQUEST, 2 = BOOTREPLY
    htype         1  Hardware address type, see ARP section in "Assigned
                     Numbers" RFC; e.g., '1' = 10mb ethernet.
    hlen          1  Hardware address length (e.g.  '6' for 10mb
                     ethernet).
   hops          1  Client sets to zero, optionally used by relay agents
                     when booting via a relay agent.
    xid           4  Transaction ID, a random number chosen by the
                     client, used by the client and server to associate
                     messages and responses between a client and a
                     server.
    secs          2  Filled in by client, seconds elapsed since client
                     began address acquisition or renewal process.
    flags         2  Flags (see figure 2).
    ciaddr        4  Client IP address; only filled in if client is in
                     BOUND, RENEW or REBINDING state and can respond
                     to ARP requests.
    yiaddr        4  'your' (client) IP address.
    siaddr        4  IP address of next server to use in bootstrap;
                     returned in DHCPOFFER, DHCPACK by server.
    giaddr        4  Relay agent IP address, used in booting via a
                     relay agent.
    chaddr       16  Client hardware address.
    sname        64  Optional server host name, null terminated string.
    file        128  Boot file name, null terminated string; "generic"
                     name or null in DHCPDISCOVER, fully qualified
                     directory-path name in DHCPOFFER.
    options     var  Optional parameters field.  See the options
                     documents for a list of defined options.
 ```
 
 В данной программе поля sname и file не заполняются. При приеме и отправке сообщений используются следующие опции (помимо pad option, end option и magic cookie):
 Subnet Mask:
 
 ```
     Code   Len        Subnet Mask
    +-----+-----+-----+-----+-----+-----+
    |  1  |  4  |  m1 |  m2 |  m3 |  m4 |
    +-----+-----+-----+-----+-----+-----+
 ```
 
 Router Option:
 
 ```
 
     Code   Len         Address 1               Address 2
    +-----+-----+-----+-----+-----+-----+-----+-----+--    |  3  |  n  |  a1 |  a2 |  a3 |  a4 |  a1 |  a2 |  ...
    +-----+-----+-----+-----+-----+-----+-----+-----+--
 ```
 
 Domain Name Server Option:
 
 ```
     Code   Len         Address 1               Address 2
    +-----+-----+-----+-----+-----+-----+-----+-----+--
    |  6  |  n  |  a1 |  a2 |  a3 |  a4 |  a1 |  a2 |  ...
    +-----+-----+-----+-----+-----+-----+-----+-----+--
 ```
 
 DHCP Message Type:
 
 ```
 
            Value   Message Type
            -----   ------------
              1     DHCPDISCOVER
              2     DHCPOFFER
              3     DHCPREQUEST
              4     DHCPDECLINE
              5     DHCPACK
              6     DHCPNAK
              7     DHCPRELEASE
 
     Code   Len  Type
    +-----+-----+-----+
    |  53 |  1  | 1-7 |
    +-----+-----+-----+
 ```
 
 Server Identifier:
 
 ```
     Code   Len            Address
    +-----+-----+-----+-----+-----+-----+
    |  54 |  4  |  a1 |  a2 |  a3 |  a4 |
    +-----+-----+-----+-----+-----+-----+
 ```
 
 IP Address Lease Time:
 
 ```
 
     Code   Len         Lease Time
    +-----+-----+-----+-----+-----+-----+
    |  51 |  4  |  t1 |  t2 |  t3 |  t4 |
    +-----+-----+-----+-----+-----+-----+
 ```
 
 Requested IP Address:
 
 ```
     Code   Len          Address
    +-----+-----+-----+-----+-----+-----+
    |  50 |  4  |  a1 |  a2 |  a3 |  a4 |
    +-----+-----+-----+-----+-----+-----+
 ```
 
Подразумевается использование только одного роутера и одного сервера имен. Важным моментом является то, что механизм аренды адресов реализован тривиально, не по RFC-стандарту. Предлагаемый/назначаемый адрес отсчитывается от адреса сервера, назначенные адреса хранятся в простом массиве, адрес выдается на максимальное время.
Сервер умеет принимать сообщения типа DHCPDISCOVER, DHCPREQUEST, DHCPRELEASE и DHCPRENEW и отвечать DHCPOFFER и DHCPACK.
Сервер проверялся работой с клиентом DHCPClient. Для проверки работы нужно запустить сервер и клиент
Результаты:
Вывод сервера

```
    MacBook-Pro-Aleksej-2:lab2s Alex$ python3 DHCPServer.py 
    DHCP Server started
    -------------------
    DISCOVER,c4:b3:01:be:55:01,0.0.0.0
    Server received Discover message from client
    This MAC address does not have an assigned IP yet
    Server is sending Offer message to client
    Sending message to client: OFFER,c4:b3:01:be:55:01,192.168.1.254
    
    REQUEST,c4:b3:01:be:55:01,192.168.1.254
    Server received a Request message from client
    Client is assigned IP address:192.168.1.254
    Server is sending Acknowledge message to client
    Sending message to client: ACK,c4:b3:01:be:55:01,192.168.1.254
```

Вывод клиента
    
```
    MacBook-Pro-Aleksej-2:lab2s Alex$ python3 DHCPClient.py 
    DHCP Client Started
    -------------------
    Sending discover from client to server
    DISCOVER,c4:b3:01:be:55:01,0.0.0.0
    
    Received message from server: OFFER,c4:b3:01:be:55:01,192.168.1.254
    Received offer from server
    offered ip: 192.168.1.254
    Sending request from client to server
    
    Received message from server: ACK,c4:b3:01:be:55:01,192.168.1.254
    ACK received from server
    Client IP Address: 192.168.1.254
    Choose From The Following
    RELEASE: 1
    RENEW:   2
    QUIT:    3

```

