# Лабораторная работа №3: Электронная почта
_Выполнили:_ Казанджи М.А. (клиент) и Никитин И.Н. (сервер) гр 3530901/70201

## Задание: 
Разработать приложение-клиент и приложение сервер электронной почты 

## Подготовка: 
Для запуска необходимо скачать Python 3.x
## Протокол
У нас есть протокол Message

`Message`
--------------------------------------
Command type (4 байта) | Body length (4 байта) | Body


* _Command type_ - код команды (0: поключение, 1: отправка сообщения, 2: получение всех писем на ящике, 
3: открытике письма, 4: удаление письма, 5: отключение)
* _Body length_ - размер тела пакета
* _Body_ - тело пакета

`Команды`
--------------------------------------
* Поключение - "_entry_ почтовый ящик формта _something@somthing.something_"
* Отправка сообщения - "_send_ почтовый ящик формта _something@somthing.something_"
* Получение всех писем на ящике - "_mailbox_"
* Открытике письма - "_open_ идентификатор письма, узнать который можно вызовом команды _mailbox_"
* Удаление письма - "_del_ идентификатор письма, узнать который можно вызовом команды _mailbox_"
* Отключение - "_quit_"

## Клиент
`Запуск`
--------------------------------------
При первом запуске клиента необходимо ввести к для запуска клиента:
```
python3 -m client_runner 127.0.0.1 2345
```
* Первый аргумент _(a.b.c.d)_ - адрес клиента (обязательный параметр)
* Второй аргумент _(p)_ - порт клиента (обязательный параметр)

`Пример работы:`
--------------------------------------
```
Добро пожаловать в почтовый сервис! 
Поддерживаемые команды: 
entry "username" - подключение к серверу по логину 
send Адрес получателся - отправить письмо, после этой команды сервер запросит заголовок письма, а потом текст
mailbox - получение всех писем на вашем ящике
open "id" - открытие письма по идентификатору (узнать id писем можно по результату команды mailbox)
del "id" - удаление письма по идентификатору (узнать id писем можно по результату команды mailbox)
quit - принудительное отключение от сервера

        
Введите команду: entry test2@email.tu
Success.
Введите команду: send test1@mail.ru
ВВедите заголовок письма: test header
Введите текст письма: some text
Success.
Введите команду: mailbox
Mailbox
id : 1
source : test1@mail.ru
destination : test2@email.tu
header : hello
text : world

Введите команду: quit
Подключение разорвано

Process finished with exit code 0

```

## Сервер

`Запуск`
--------------------------------------
Для запуска сервера необзодимо ввести команду в консоль: 
```
python3 -m server_runner 127.0.0.1 2345
```
* Первый аргумент _(a.b.c.d)_ - адрес сервера (обязательный параметр)
* Второй аргумент _(p)_ - порт сервера (обязательный параметр)

`Пример работы: (на основе запросов из примера работы клиента)`
--------------------------------------
```
Listening for connections on 127.0.0.1:2345...
[-] Connected to 127.0.0.1:63218
[-] Connected to 127.0.0.1:63219
<-2145590898> [0]: conteht={'name': 'test1@mail.ru'}
{<socket.socket fd=488, family=AddressFamily.AF_INET, type=SocketKind.SOCK_STREAM, proto=0, laddr=('127.0.0.1', 2345), raddr=('127.0.0.1', 63218)>: 'test1@mail.ru'}
{'test1@mail.ru': []}
<-2145590894> [0]: conteht={'name': 'test2@email.tu'}
{<socket.socket fd=488, family=AddressFamily.AF_INET, type=SocketKind.SOCK_STREAM, proto=0, laddr=('127.0.0.1', 2345), raddr=('127.0.0.1', 63218)>: 'test1@mail.ru', <socket.socket fd=436, family=AddressFamily.AF_INET, type=SocketKind.SOCK_STREAM, proto=0, laddr=('127.0.0.1', 2345), raddr=('127.0.0.1', 63219)>: 'test2@email.tu'}
{'test1@mail.ru': [], 'test2@email.tu': []}
<-2145590894> [1]: conteht={'to': 'test1@mail.ru', 'header': 'test header', 'text': 'some text'}
{'test1@mail.ru': [<src.lab3.email.protocol.mail.Mail object at 0x01CF1DC0>], 'test2@email.tu': []}
<-2145590898> [1]: conteht={'to': 'test2@email.ru', 'header': 'header', 'text': 'hello'}
<-2145590898> [1]: conteht={'to': 'test2@email.tu', 'header': 'hello', 'text': 'world'}
{'test1@mail.ru': [<src.lab3.email.protocol.mail.Mail object at 0x01CF1DC0>], 'test2@email.tu': [<src.lab3.email.protocol.mail.Mail object at 0x01CF1E98>]}
<-2145590894> [2]: conteht=None
```
