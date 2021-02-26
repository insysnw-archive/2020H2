# Лабораторная работа №2: SNMP сервер
_Выполнил:_ Никитин И.Н. гр 3530901/70201

## Задание: 
Разработать NTP-клиент на основе стандартного протокола NTP. 

## Запусук
Для работы нужен Python3.7 и выше.

Для запуска работы нужно ввести команду 
"_python snmp_server.py_ [опции]"

Реализовано несколько опций, такие как:
* --host [IP] - IP адрес для подключения
* --port [port] - порт для работы
* --repository [path] - путь до файла c переменными (формат TOML)
* --engine [agent] - идентификатор агента SNMP

Опции являются необязательными.

## Проверка работы

Для проверки работы использовали файл _test.toml_, в котором указаны нельсколько переменных. Для проверка осуществлялась
командами snmpget, snmpgetnext, snmpwalk из пакета snmp.


* Получим пару переменных при помощи _snmpget_
* Получим первую переменную через GetNext запрос при помощи _snmpgetnext_
* Обойдем весь репозиторий при помощи _snmpwalk_


```
patron@Callisto:~$ snmpget -u nobody localhost 1.5.4.55.66
iso.5.4.55.66 = STRING: "Test string"
patron@Callisto:~$ snmpget -u nobody localhost 1.5.45.55.6.12
iso.5.45.55.6.12 = IpAddress: 0.0.0.0
patron@Callisto:~$ snmpgetnext -u nobody localhost 1.5
iso.5.4.55.66 = STRING: "Test string"
patron@Callisto:~$ snmpwalk -u nobody localhost 1.5
iso.5.4.55.66 = STRING: "Test string"
iso.5.4.55.67 = STRING: "Other text"
iso.5.4.56.66 = INTEGER: 1010
iso.5.4.56.67 = INTEGER: 2021
iso.5.45.55.6.12 = IpAddress: 0.0.0.0
iso.5.45.55.6.13 = IpAddress: 192.168.0.1
iso.5.45.55.6.13 = No more variables left in this MIB View (It is past the end of the MIB tree)
patron@Callisto:~$
```
## Описание протокола
SNMP — стандартный интернет-протокол для управления устройствами в IP-сетях на основе архитектур TCP/UDP. Протокол 
обычно используется в системах сетевого управления для контроля подключённых к сети устройств на предмет условий.
SNMP состоит из набора стандартов для сетевого управления, включая протокол прикладного уровня, схему баз данных и 
набор объектов данных.

`Формат SNMP-сообщения`

Сообщение содержит такие поля, как:
* Версия
* Пароль
* Тип PDU (Protocol Data Unit)
* get/set заголовок
* Переменные для get/set