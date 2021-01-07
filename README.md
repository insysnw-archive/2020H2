Лабораторная №2 пункт с:  DHCP Сервер
======================================

Сборка
--------------------------------------

Сборка осуществляется с помощью Maven и JDK 1.8.
Для удобства в репозиторий помещён [Maven Wrapper](https://github.com/takari/maven-wrapper).
Эта утилита скачает необходимую версию Maven.
Таким образом в системе должен присутствовать только JDK 1.8.

Команда сборки:

    ./mvnw package

Запуск
--------------------------------------

**ВАРИАНТ 1**   

1. Зайти в папку с проектом
2. В командную строчку ввести команду:  
`java -jar out/artifacts/DHCP_jar/2020H2.jar [-address ADDRESS] [-listen LISTEN] [-port PORT] [-broadcast BROADCAST] <-config CONFIG>`

**ВАРИАНТ 2**   
Запустить сервер можно тут же на месте, запустив после сборки команду:

    ./mvnw exec:java -Dexec.args='[-address ADDRESS] [-listen LISTEN] [-port PORT] [-broadcast BROADCAST] <-config CONFIG>'

Описание опций:
 - *address* -- идентификатор сервера в виде IP адреса (по смыслу должен быть адрес хоста)
 - *listen* -- адрес, на котором следует прослушивать сообщения UDP
 - *port* -- порт, который следует прослушивать
 - *broadcast* -- адрес, на который следует посылать широковещательные сообщения UDP
 - *config* -- обязательная опция; файл, где описана конфигурация сервера

Кроме аргументов необходимо иметь файл с описанием конфигурации на языке TOML.
Ниже приведён пример конфигурации, с помощью которого проводилось ручное тестирование.

```toml
server-hostname = 'dhcp-host'

range.start = '10.23.0.100' # начало промежутка выделяемых адресов
range.stop = '10.23.0.150'  # конец промежутка выделяемых адресов

# Некоторые опции, которые стоит указать
[options]
router = [ '10.23.0.2' ]
broadcast = '10.23.1.255'
mask = '255.255.254.0'
domain-name = 'example.com'
domain-nameserver = [ '8.8.8.8', '8.8.4.4' ]

# Список хостов со статическими адресами
[[host]]
hardware-ethernet = '52:54:00:c2:89:1e'
address = '10.23.0.51'

# Опции только для этого хоста
[host.options]
hostname = 'dummy-host'
```

Реализация
--------------------------------------

Был реализован [RFC 2131](https://tools.ietf.org/html/rfc2131)
(с допущением, что DHCP сервер обслуживает только одну подсеть) и
часть опции из [RFC 2132](https://tools.ietf.org/html/rfc2132).

Тестирование
--------------------------------------

Для полноценного тестирования было бы здорово настроить интерфейс реальной ОС с помощью этого DHCP сервера.
Было решено использовать сеть из виртуальных машин. Вот что было для этого сделано.

### Создание сетевого интерфейса

Чтобы не пугать участников локальной сети DHCP своим сервером, был создан виртуальный сетевой интерфейс vbr0.

```sh
ip link add vrt0 type dummy
ip link add name vbr0 type bridge
ip link set dev vbr0 up
ip link set dev vrt0 master vbr0
ip addr add 10.23.0.2/23 brd 255.255.254.0 dev vbr0
ip link set vbr0 address 00:fc:34:12:00:01
```

Вывод команды `ip addr show dev vbr0`:

```
17: vbr0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default qlen 1000
    link/ether 00:fc:34:12:00:01 brd ff:ff:ff:ff:ff:ff
    inet 10.23.0.2/23 brd 255.255.254.0 scope global vbr0
       valid_lft forever preferred_lft forever
    inet6 fe80::d0f8:d7ff:fef2:d2c/64 scope link 
       valid_lft forever preferred_lft forever
```

### Создание виртуальных машин

Для работы с виртуальными машинами был использован [менеджер виртуальных машин](https://virt-manager.org/) от RedHat.
Было создано две виртуальных машины на основе дистрибутива [Alpine Linux](https://alpinelinux.org) Virtual 3.12.1.
Обе виртуальные машины были подключены к созданному ранее мосту vbr0.
На каждой виртуальной машине был настроен интерфейс eth0 на получение настроек по DHCP (команда `setup-interfaces`).

После этого, командой `ip link show dev eth0` был получен ethernet (MAC) адрес на каждой виртуальной машине.

Адреса виртуальных машин:
1. 52:54:00:bb:40:b4
2. 52:54:00:c2:89:1e

Адреса не совпали с заданным ранее адресом для vbr0 и друг другом, как и следовало.
Второй адрес был записан в конфигурационный файл для статического определения IP адреса.

### Ловля пакетов

Для отладки любого протокола существуют инструменты для дампа передаваемых данных.
Для данной работы была использована утилита **tcpdump**, которая была запущена со следующими параметрами.

    tcpdump -vv -i vbr0 port 67 or port 68

По протоколу DHCP ответы *следует* принимать на порту 68.
Фильтр можно было и более точный прописать, но в данной ситуации
не ожидается получить что-то от другого сетевого взаимодействия.

Позже мы вернёмся к выводу данной команды.

### Запуск сервера

Запустим сервер через Maven от имени root (чтобы получить доступ к well-known портам).

    sudo ./mvnw exec:java -Dexec.args='-config=config.toml -address=10.23.0.2 -listen=0.0.0.0 -broadcast=10.23.1.255'

### Запуск клиентов

Теперь необходимо осуществить поднятие сетевых интерфейсов на виртуальных машинах.
Для этого на каждой виртуальной машине следует ввести команду.

    ifup eth0

Вывод после работы данной команды:

Машина 1:
```
localhost:~# ifup eth0
udhcpc: started, v1.31.1
udhcpc: sending discover
udhcpc: sending select for 10.23.0.151
udhcpc: lease of 10.23.0.151 obtained, lease time 1000

localhost:~# cat /etc/resolv.conf
search example.com
nameserver 8.8.8.8
nameserver 8.8.4.4

localhost:~# ip addr show dev eth0
2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP qlen 1000
    link/ether 52:54:00:bb:40:b4 brd ff:ff:ff:ff:ff:ff
    inet 10.23.0.151/23 brd 10.23.1.255 scope global eth0
       valid_lft forever preferred_lft forever
    inet6 fe80::5054:ff:febb:40b4/64 scope link 
       valid_lft forever preferred_lft forever
```

Машина 2:
```
localhost:~# ifup eth0
udhcpc: started, v1.31.1
udhcpc: sending discover
udhcpc: sending select for 10.23.0.51
udhcpc: lease of 10.23.0.51 obtained, lease time 1000

localhost:~# cat /etc/resolv.conf
search example.com
nameserver 8.8.8.8
nameserver 8.8.4.4

localhost:~# ip addr show dev eth0
2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP qlen 1000
    link/ether 52:54:00:c2:89:1e brd ff:ff:ff:ff:ff:ff
    inet 10.23.0.51/23 brd 10.23.1.255 scope global eth0
       valid_lft forever preferred_lft forever
    inet6 fe80::5054:ff:fec2:891e/64 scope link 
       valid_lft forever preferred_lft forever
```

Это пока нарушает логическую цепочку, но часть выводов от виртуальных машин была передана через команды:

    nc -l 10.23.0.2 2323 # хост

    ip addr show eth0 | nc 10.23.0.2 2323 # гость

Но закроем на это глаза и порадуемся выводу команды ping (опять передав его через nc).

Машина 2:
```
localhost:~# ping 10.23.0.2
PING 10.23.0.2 (10.23.0.2): 56 data bytes
64 bytes from 10.23.0.2: seq=0 ttl=64 time=0.139 ms
64 bytes from 10.23.0.2: seq=1 ttl=64 time=0.314 ms
64 bytes from 10.23.0.2: seq=2 ttl=64 time=0.325 ms
64 bytes from 10.23.0.2: seq=3 ttl=64 time=0.218 ms
64 bytes from 10.23.0.2: seq=4 ttl=64 time=0.218 ms
64 bytes from 10.23.0.2: seq=5 ttl=64 time=0.309 ms
64 bytes from 10.23.0.2: seq=6 ttl=64 time=0.281 ms
64 bytes from 10.23.0.2: seq=7 ttl=64 time=0.274 ms
```

Попробуем выйти в Интернет. Сначала убедимся, что опция `router`
применилась и в таблице маршрутизации у нас есть нужный маршрут.

Машина 1:
```
localhost:~# ip route
default via 10.23.0.2 dev eth0  metric 202 
10.23.0.0/23 dev eth0 scope link  src 10.23.0.151
```

Маршрут верный. Значение default это адрес хостовой ОС.

Машина 1:
```
localhost:~# ping google.ru
ping: bad address 'google.ru'
```

Адрес почему-то не ресолвится. Дело в том, что на хостовой системе не включена опция форвардинга.
Для этого необходимо включить `net.ipv4.ip_forward` в настройках ядра системы.
Но не будем этого делать, так как DHCP сервер работает, а большего и не надо.

### Анализ пойманных пакетов

Вернёмся к утилите tcpdump и посмотрим, что она выдала.

Всего она поймала 39 DHCP пакетов.

Дампы DHCP Discover запроса и ответа DHCP Offer.

```
00:25:36.824969 IP (tos 0x0, ttl 64, id 0, offset 0, flags [none], proto UDP (17), length 328)
    0.0.0.0.bootpc > 255.255.255.255.bootps: [udp sum ok] BOOTP/DHCP, Request from 52:54:00:bb:40:b4 (oui Unknown), length 300, xid 0xbe19fb6c, secs 3, Flags [none] (0x0000)
	  Client-Ethernet-Address 52:54:00:bb:40:b4 (oui Unknown)
	  Vendor-rfc1048 Extensions
	    Magic Cookie 0x63825363
	    DHCP-Message Option 53, length 1: Discover
	    Client-ID Option 61, length 7: ether 52:54:00:bb:40:b4
	    MSZ Option 57, length 2: 576
	    Parameter-Request Option 55, length 7: 
	      Subnet-Mask, Default-Gateway, Domain-Name-Server, Hostname
	      Domain-Name, BR, NTP
	    Vendor-Class Option 60, length 12: "udhcp 1.31.1"
	    Hostname Option 12, length 9: "localhost"
00:25:36.829242 IP (tos 0x0, ttl 64, id 52384, offset 0, flags [DF], proto UDP (17), length 604)
    Mars.bootps > 10.23.1.255.bootpc: [bad udp cksum 0x1888 -> 0x38aa!] BOOTP/DHCP, Reply, length 576, xid 0xbe19fb6c, Flags [none] (0x0000)
	  Your-IP 10.23.0.151
	  Server-IP Mars
	  Client-Ethernet-Address 52:54:00:bb:40:b4 (oui Unknown)
	  sname "dhcp-host"
	  Vendor-rfc1048 Extensions
	    Magic Cookie 0x63825363
	    Server-ID Option 54, length 4: Mars
	    DHCP-Message Option 53, length 1: Offer
	    Lease-Time Option 51, length 4: 1000
	    Subnet-Mask Option 1, length 4: 255.255.254.0
	    Default-Gateway Option 3, length 4: Mars
	    Domain-Name-Server Option 6, length 8: 8.8.8.8,8.8.4.4
	    Domain-Name Option 15, length 11: "example.com"
	    BR Option 28, length 4: 10.23.1.255
	    MSG Option 56, length 7: "offered"
```

В этом выводе всё соответствует ожиданиям, только слегка напрягает `bad udp cksum` в ответе.

По запросу видно, что это первая виртуальная машина. Кроме этого server-id помечен как Mars.
Это показывает особенность tcpdump. Он получил имя хоста с адресом 10.23.0.2.

Далее рассмотрим пару DHCP Request и DHCP Ack

```
00:25:36.829589 IP (tos 0x0, ttl 64, id 0, offset 0, flags [none], proto UDP (17), length 331)
    0.0.0.0.bootpc > 255.255.255.255.bootps: [udp sum ok] BOOTP/DHCP, Request from 52:54:00:bb:40:b4 (oui Unknown), length 303, xid 0xbe19fb6c, secs 3, Flags [none] (0x0000)
	  Client-Ethernet-Address 52:54:00:bb:40:b4 (oui Unknown)
	  Vendor-rfc1048 Extensions
	    Magic Cookie 0x63825363
	    DHCP-Message Option 53, length 1: Request
	    Client-ID Option 61, length 7: ether 52:54:00:bb:40:b4
	    Requested-IP Option 50, length 4: 10.23.0.151
	    Server-ID Option 54, length 4: Mars
	    MSZ Option 57, length 2: 576
	    Parameter-Request Option 55, length 7: 
	      Subnet-Mask, Default-Gateway, Domain-Name-Server, Hostname
	      Domain-Name, BR, NTP
	    Vendor-Class Option 60, length 12: "udhcp 1.31.1"
	    Hostname Option 12, length 9: "localhost"
00:25:36.830741 IP (tos 0x0, ttl 64, id 52385, offset 0, flags [DF], proto UDP (17), length 604)
    Mars.bootps > 10.23.1.255.bootpc: [bad udp cksum 0x1888 -> 0x73ac!] BOOTP/DHCP, Reply, length 576, xid 0xbe19fb6c, Flags [none] (0x0000)
	  Your-IP 10.23.0.151
	  Server-IP Mars
	  Client-Ethernet-Address 52:54:00:bb:40:b4 (oui Unknown)
	  sname "dhcp-host"
	  Vendor-rfc1048 Extensions
	    Magic Cookie 0x63825363
	    Server-ID Option 54, length 4: Mars
	    DHCP-Message Option 53, length 1: ACK
	    Lease-Time Option 51, length 4: 1000
	    RN Option 58, length 4: 990
	    RB Option 59, length 4: 1000
	    Subnet-Mask Option 1, length 4: 255.255.254.0
	    Default-Gateway Option 3, length 4: Mars
	    Domain-Name-Server Option 6, length 8: 8.8.8.8,8.8.4.4
	    Domain-Name Option 15, length 11: "example.com"
	    BR Option 28, length 4: 10.23.1.255
	    MSG Option 56, length 8: "assigned"
```

Опять странный `bad udp cksum` в ответе, а в остальном всё правильно.

Пример обновления адреса второй виртуальной машины:

```
00:42:21.291779 IP (tos 0x0, ttl 64, id 0, offset 0, flags [none], proto UDP (17), length 331)
    0.0.0.0.bootpc > 255.255.255.255.bootps: [udp sum ok] BOOTP/DHCP, Request from 52:54:00:c2:89:1e (oui Unknown), length 303, xid 0xd7642c4f, Flags [none] (0x0000)
	  Client-Ethernet-Address 52:54:00:c2:89:1e (oui Unknown)
	  Vendor-rfc1048 Extensions
	    Magic Cookie 0x63825363
	    DHCP-Message Option 53, length 1: Request
	    Client-ID Option 61, length 7: ether 52:54:00:c2:89:1e
	    Requested-IP Option 50, length 4: 10.23.0.51
	    Server-ID Option 54, length 4: Mars
	    MSZ Option 57, length 2: 576
	    Parameter-Request Option 55, length 7: 
	      Subnet-Mask, Default-Gateway, Domain-Name-Server, Hostname
	      Domain-Name, BR, NTP
	    Vendor-Class Option 60, length 12: "udhcp 1.31.1"
	    Hostname Option 12, length 9: "localhost"
00:42:21.292350 IP (tos 0x0, ttl 64, id 35375, offset 0, flags [DF], proto UDP (17), length 604)
    Mars.bootps > 10.23.1.255.bootpc: [bad udp cksum 0x1888 -> 0xe33e!] BOOTP/DHCP, Reply, length 576, xid 0xd7642c4f, Flags [none] (0x0000)
	  Your-IP 10.23.0.51
	  Server-IP Mars
	  Client-Ethernet-Address 52:54:00:c2:89:1e (oui Unknown)
	  sname "dhcp-host"
	  Vendor-rfc1048 Extensions
	    Magic Cookie 0x63825363
	    Server-ID Option 54, length 4: Mars
	    DHCP-Message Option 53, length 1: ACK
	    Lease-Time Option 51, length 4: 1000
	    RN Option 58, length 4: 990
	    RB Option 59, length 4: 1000
	    Subnet-Mask Option 1, length 4: 255.255.254.0
	    Default-Gateway Option 3, length 4: Mars
	    Domain-Name-Server Option 6, length 8: 8.8.8.8,8.8.4.4
	    Hostname Option 12, length 10: "dummy-host"
	    Domain-Name Option 15, length 11: "example.com"
	    BR Option 28, length 4: 10.23.1.255
	    MSG Option 56, length 8: "assigned"
```

В этом выводе можно увидеть опцию `hostname`, которую мы специально указывали для этой машины в
конфигурации DHCP сервера.

Попробуем теперь остановить DHCP клиент на одной из машин.

Вторая виртуальная машина:

    ifdown eth0

В ответ на эту команду был остановлен DHCP клиент и сервер получил ответ следующего содержания.

```
01:11:12.457103 IP (tos 0x0, ttl 64, id 60535, offset 0, flags [DF], proto UDP (17), length 328)
    10.23.0.51.bootpc > Mars.bootps: [udp sum ok] BOOTP/DHCP, Request from 52:54:00:c2:89:1e (oui Unknown), length 300, xid 0x188b4e10, secs 237, Flags [none] (0x0000)
	  Client-IP 10.23.0.51
	  Client-Ethernet-Address 52:54:00:c2:89:1e (oui Unknown)
	  Vendor-rfc1048 Extensions
	    Magic Cookie 0x63825363
	    DHCP-Message Option 53, length 1: Release
	    Client-ID Option 61, length 7: ether 52:54:00:c2:89:1e
	    Server-ID Option 54, length 4: Mars
```

А в логе сервера появилась запись:

```
01:11:12.457 [lab2.dhcp.Server.main()] INFO lab2.dhcp.Server - request Release from 52:54:00:C2:89:1E
```
