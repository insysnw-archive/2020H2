# Лабораторная №2

В данной ветке находятся DNS клиент и очень упрощённый вариант SNMPv3 сервера.

Программы сервера и клиента написаны на Python 3. Для выполнения должен присутствовать интерпритатор python3.7 (не тестировалось на более низких версиях).

## Сервер SNMPv3

Сервер является частичной реализацией [RFC3411](https://tools.ietf.org/html/rfc3411) и остальных документов RFC, описывающих SNMPv3.

### Использование

Сервер запускается так:

```
./snmp_server.py [опции]
```

Опции:
  - **--host HOST** IP адрес, на котором следует прослушивать UDP сообщения (по умолчанию `0.0.0.0`);
  - **--port PORT** номер UDP порта серверного сокета (по умолчанию `161`);
  - **--repository REPOSITORY** путь до файла формта [TOML](https://toml.io) с переменными и их параметрами (по умолчанию `agent.toml`);
  - **--engine ENGINE** уникальный идентификатор агента SNMP (по умолчанию `ssnmp`).

Все опции тут не обязательны.

### Демонстрация работы

Для демонстрации работы SNMP сервера был использован пакет [net-snmp]. В его состав входят утилиты snmpget, snmpwalk, snmpnext и snmpset, которые использовались для проверки написанного сервера.

В данном репозитории в файле **[agent.toml](agent.toml)** объявлены некоторые переменные, которые были использованы для данной демонстрации.

Запустим сервер:

    ./snmp_server

Пусть сервер работает, общение с ним будет проходить в другом терминале с использованием утилит пакета [net-snmp].

Попробуем получить значение переменной 1.3.4.23.66:

```
nik@localhost:~$ snmpget -u nobody localhost 1.3.4.23.66
iso.3.4.23.66 = STRING: "hello world"
```

Попробуем также получить один из IP адресов

```
nik@localhost:~$ snmpget -u nobody localhost 1.3.4.23.66 1.3.45.67.7.123
iso.3.4.23.66 = STRING: "hello world"
iso.3.45.67.7.123 = IpAddress: 127.0.0.1
```

Попробуем получить первую переменную через GetNext запрос и повторим для её OID:

```
nik@localhost:~$ snmpgetnext -u nobody localhost 1.3
iso.3.4.23.66 = STRING: "hello world"
nik@localhost:~$ snmpgetnext -u nobody localhost iso.3.4.23.66
iso.3.4.23.67 = INTEGER: 23
```

Чтобы не делать весь обход по репозиторию переменных вручную, воспользуемся командой snmpwalk:

```
nik@localhost:~$ snmpwalk -u nobody localhost 1.3
iso.3.4.23.66 = STRING: "hello world"
iso.3.4.23.67 = INTEGER: 23
iso.3.45.31 = Timeticks: (3891508323) 450 days, 9:44:43.23
iso.3.45.67.7.123 = IpAddress: 127.0.0.1
iso.3.45.67.7.124 = IpAddress: 127.0.0.1
iso.3.45.67.7.124 = No more variables left in this MIB View (It is past the end of the MIB tree)
```

Попробуем поменять значение одной из переменных:

```
nik@localhost:~$ snmpset -u nobody localhost 1.3.45.67.7.123 a 10.0.0.1
Error in packet.
Reason: notWritable (That object does not support modification)
```

Эта переменная не значится как `mutable` в файле репозитория. Попробуем то же самое на втором `mutable` IP адресе и проверим:

```
nik@localhost:~$ snmpset -u nobody localhost 1.3.45.67.7.124 a 10.0.0.1
iso.3.45.67.7.124 = IpAddress: 10.0.0.1
nik@localhost:~$ snmpget -u nobody localhost 1.3.45.67.7.124
iso.3.45.67.7.124 = IpAddress: 10.0.0.1
```

Значение поменялось. Попробуем присвоить этой переменной другой тип:

```
nik@localhost:~$ root@python:/data/github/GolzitskyNikolay/2020H2/lab2# snmpset -u nobody localhost 1.3.45.67.7.124 i 42
Error in packet.
Reason: wrongType (The set datatype does not match the data type the agent expects)
```

Не удалось. Тип переменной соответствует тому, что объявлен в репозитории переменных.

## Клиент DNS

DNS клиент реализует протокол, описанный в документе [RFC1035](https://tools.ietf.org/html/rfc1035). Ответ сервера трансформирует в JSON объект, который может прочесть человек с меньшим рудом, чем HEX представление бинарного содержимого UDP дейтаграммы.

### Использование

Вызов клиента осуществляется так:

```
./dns_client.py [опции] domain
```

Опции:
  - **--host HOST** имя хоста DNS сервера (по умолчанию `1.1.1.1`);
  - **--port PORT** порт DNS сервера (по умолчанию `53`);
  - **--type TYPE** тип DNS записи, может иметь значения: `A`, `NS`, `MX` (по умолчанию `A`).

Аргументы:
  - **domain** доменное имя, по которому будет осуществляться запрос.

Несмотря на небольшое количество поддерживаемых типов записей, их количество можно легко расширить, используюя существующие абстракции над типами записей в скрипте **[dns_client.py](dns_client.py)** в случае необходимости.

### Демонстрация работы

Попробуем получить IP адрес Яндекса, чтобы убрать всё лишнее, воспользуемся утилитой [jq](https://stedolan.github.io/jq):

```
nik@localhost:~$ ./dns_client.py yandex.ru | \
    jq -r '.answers[] | select(.type = "A").address'
5.255.255.55
77.88.55.60
5.255.255.60
77.88.55.66
```

Теперь получим адрес почтового сервера того же Яндекса:

```
nik@localhost:~$ ./dns_client.py --type MX yandex.ru | jq '.answers[] | select(.type = "MX")'
{
  "ttl": 83,
  "type_id": 15,
  "type": "MX",
  "name": "yandex.ru.",
  "preference": 10,
  "exchange": "mx.yandex.ru."
}
```

[net-snmp]: http://www.net-snmp.org
