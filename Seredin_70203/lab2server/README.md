# DHCP server

``` bash
$python3 server.py
```
default: IP = 0.0.0.0 и port = 67.

---

## Description

**Package**
![Markdown Logo](https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Fwww.researchgate.net%2Fprofile%2FJaeseung_Song%2Fpublication%2F263813522%2Ffigure%2Fdownload%2Ffig10%2FAS%3A667882941866005%401536247110658%2FPacket-format-for-DHCP.png&f=1&nofb=1)

1. **OpCode** указывает нам на тип DHCP-сообщения. 01 - запрос от клиента к серверу. 02 - является ответом от сервера.
2. **Hardware Type** тип адреса на канальном уровне.
3. **Hardware Length** длина аппаратного адреса в байтах.
4. **Hops** количество промежуточных маршрутизаторов.
5. **Transaction ID** клиент генерирует значение для этого поля.
6. **Seconds Elapsed** время в секундах с момента начала процесса получения IP-адреса.
7. **Flags** поле для флагов.
8. **Client, Your, Server, Gateway IP Address**  IP-адреса.
9. **Client Hardware Address** записывается MAC-адрес клиента.
10. **Server Host Name**  доменное имя/имя хоста.
11. **Boot File** служит указателем для бездисковых рабочих станций.
12. **Options** основные настройки.
---

После старта сервер ожидает. После получения широковещательного запроса сервер обрабатывает информацию и высылает определенный ответ. В ответ на DHCPDISCOVER сервер генерирует пакет DHCPOFFER.В пакет записывается IP адрес в определенном промежутке. Данный IP адрес записывается в список адресов, чтобы при генерации следующего IP не сгенерировать такой же. В ответ на DHCREQUEST сервер генерирует DHCPACK предназначенные для данного клиента.

---

При тестировании сервера выводится вся ключевая информация об IP, типе сообщения и кто отправитель.

``` bash
$python server.py
    DHCP server running at 0.0.0.0 : 67
    message received from:  ('192.168.0.100', 68)
    message type: 1
    DHCPDISCOVER accepted
    send DHCPOFFER to:  ('192.168.0.100', 68)
    offered address:  [0, 0, 0, 1]
    message received from:  ('192.168.0.100', 68)
    message type: 3
    DHCPREQUEST accepted
    send DHCPACK to:  ('192.168.0.100', 68)
    assigned address:  [0, 0, 0, 1]
    message received from:  ('192.168.0.100', 68)
    message type: 3
    DHCPREQUEST accepted
    send DHCPACK to:  ('192.168.0.100', 68)
    assigned address:  [0, 0, 0, 2]
```
