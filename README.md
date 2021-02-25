# Лабораторная работа №2: NTP клиент
_Выполнил:_ Никитин И.Н. гр 3530901/70201

## Запусук
Для работы нужен Python3

Для запуска работы нужно ввести команду 
* Сервер - "_pithon3 server.py_ [ip][port]"
* Клиент - "_pithon3 client.py_ [ip][port]"

## Описание протокола

`Формат пакета`
* Пакет от клиента содержит поля: длина сообщения, саом сообщение
* Пакет от сервера содержит поля: длина имени, имя, длина сообщения, сообщение

Длина сообщения и имени - 5 байт (задано в коде).
Следовательно, максимальное количество симвлов равно 2^40.

В начале запрашивается имя пользователя, далее отправляется пакет на сервер.
Затем возможно общение всех подключенных пользователей.
При получении пакета клиентом формируется строка типа "<HH:MM> [nickname] _message_"
