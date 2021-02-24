# Лабораторная 3


## Система электронной почты. Клиент.


## Инструкция по использованию

Проект написаны на Kotlin JVM и требуют JDK 1.8 для сборки и JRE 1.8 для работы. Сборка осуществляется через систему
Gradle. Дополнительно скачивать Gradle не нужно. В репозитории хранятся скрипты Gradle Wrapper, которые могут скачать
нужную версию Gradle в случае необходимости. Для запуска клиента необходимо ввести: Linux - ./gradlew run, Windows - gradlew run.
=======
Все пакеты, состоят из двух частей:

##### Первый байт - число. Если этот пакет от клиента серверу - это команда. Если это пакет от сервера клиенту - результат выполнения запроса.

Возможные значения:

От клиента серверу (команды):

- 0 - Авторизация клиента на сервере.
- 1 - Отправка письма.
- 2 - Запрос на получение всех имеющихся писем.
- 3 - Удаление с почтового сервера одного письма.
- 4 - Выход с сервера.

От сервера клиенту (результат выполнения запроса клиента):

- 0 - Успешный обработка запроса клиента.
- 1 - Ошибка.

##### Вторая часть пакета - строка, в которой содержится информация в формате Json.

После отправки любой команды из списка выше на сервер, сервер отвечает пакетом, который содержит команду с результатом
выполнения запроса клиента и сообщение в формате json с информацией о выполненнии запроса:

`
{
"message": "Success!"
}
`

* Авторизация - для авторизации нужно послать на сервер пакет с командой 0 и свой email в формате json:

`
{
"name": "example@example.example"
}
`

Поддерживаются форматы email по стандарту RFC 5321.

`
{
"message": "Incorrect Email"
}
`

В случае успеха вернется приветственное сообщение.

* Отправка письма

Письма обрабатываются в формате json:

`
{
"to": "recipient",
"header": "email's header",
"content": "email's contenet",
"time": "dispatch time"
}
`

* Получение всех писем.

Для получения всех писем нужно передать только команду (2). Письма приходят в формате json:

`
[
{
"from": "sender1",
"to": "recipient1",
"header": "email's header1",
"content": "email's contenet1",
"time": "dispatch time1"
}, {
"from": "sender2",
"to": "recipient2",
"header": "email's header2",
"content": "email's contenet2",
"time": "dispatch time2"
}
]
`

* Удаление письма

Для удаление одного пиьсма с сервера нужно передать его id в формате json:
`
{
"index": 1 }
`

* Выход с сервера

Для выхода с сервера нужно передать только команду (4).

## Реализация клиента

#### Основные возможности:

1) Установление соединения с сервером
2) Передача электронного письма на сервер для другого клиента
3) Проверка состояния своего почтового ящика
4) Получение конкретного письма с сервера
5) Разрыв соединения
6) Обработка ситуации отключения клиента сервером

По умолчанию клиент запускаeтся на адресе 127.0.0.1 и на порту - 9999, это можно изменить с помощью аргументов командной
строки `<host> <port>` или `<host>`.

## Использование программы

````
help - show help.
login <email> - command to authorize in server.
send -to <reciever> -header <mail header> -content <mail content> - command to send mail.
read - get all mails.
delete <id> - delete mail by id. (id in square brackets)
quit - quit from server and unauthorize.
exit - to quit and exit program
````

Как видно из информации выше клиент поддерживает несколько форматов команд. Каждый ответ от сервера выводится в
формате ``<result>: <message>``. Поддерживается выход из аккаунта и последующая повторная авторизация. Если введена
неверная команда или неверный формат команды выводится соответсвующее сообщение.

Для отправки письма с клиент используется команда ``send``. После флага `-content` идет текст письма. Слова в письме
можно разделять пробелами и другими символами.

Для завершения программы используется команда ``exit``. Если клиент в этот момент авторизован на сервере, клиент
отключается от сервера и программа завершает свою работу.

Вывод всех писем происходит с помощью команды ``read``. Письма выводятся в формате:

````
[id] from: <sender>
    to: <reciever>
    header: <mail header>
    content: <mail content>
    time: <time>
````

Если на сервере на текущий момент нет писем, выводится соответсвующее сообщение.

### Пример использования программы

````
egmail> commands:
    help - show help.
    login <email> - command to authorize in server.
    send -to <reciever> -header <mail header> -content <mail content> - command to send mail.
    read - get all mails.
    delete <id> - delete mail by id. (id in square brackets)
    quit - quit from server and unauthorize.
    exit - to quit and exit program
egmail> login qwerty@qwerty.com
egmail> Success: Welcome to EgMail
egmail> read
egmail> Success: Mailbox is empty
egmail> send -to asdf@asdf.com -header diplom -content privet I want to pass diplom
egmail> Success: Success!
egmail> read
egmail> Success: 
[0] from: qwerty@qwerty.com
    to: asdf@asdf.com
    header: diplom
    content: privet I want to pass diplom
    time: 17.01.2021 12:39

egmail> quit
egmail> Success: Goodbye, qwerty@qwerty.com
egmail> loguin asdf@asdf.com
egmail> Incorrect command: loguin
egmail> login asdf@asdf.com
egmail> Success: Welcome to EgMail
egmail> read
egmail> Success: 
[0] from: qwerty@qwerty.com
    to: asdf@asdf.com
    header: diplom
    content: privet I want to pass diplom
    time: 17.01.2021 12:39

egmail> send -to qwerty@qwerty.com -header diplom -content privet I want to pass diplom too!!
egmail> Success: Success!
egmail> read
egmail> Success: 
[0] from: qwerty@qwerty.com
    to: asdf@asdf.com
    header: diplom
    content: privet I want to pass diplom
    time: 17.01.2021 12:39

[1] from: asdf@asdf.com
    to: qwerty@qwerty.com
    header: diplom
    content: privet I want to pass diplom too!!
    time: 17.01.2021 12:41

egmail> delete 0
egmail> Success: Email with id 0 deleted!
egmail> read
egmail> Success: 
[0] from: asdf@asdf.com
    to: qwerty@qwerty.com
    header: diplom
    content: privet I want to pass diplom too!!
    time: 17.01.2021 12:41

egmail> quit
egmail> Success: Goodbye, asdf@asdf.com
egmail> exit
Process finished with exit code 0

````
