# Лабораторная работа 1

Сборка
--------------------------------------

Сборка осуществляется с помощью Maven и JDK 1.8.
Для удобства в репозиторий помещён [Maven Wrapper](https://github.com/takari/maven-wrapper).
Эта утилита скачает необходимую версию Maven.
Таким образом в системе должен присутствовать только JDK 1.8.

Команда сборки:

    ./mvnw package

После сборки в каждом сабмодуле будет сгенерирована папка таргет, внутри которой будет jat-файл.


Запуск
--------------------------------------

Запустить сервер можно из основного модуля выполнив команду:

`java -jar <module>/target/<module>-1.0-SNAPSHOT.jar <аргументы>`

Исполняемых всего 4 модуля:
1. server
2. client
3. nio-server
4. nio-client

*Модули с приставкой nio - модули на неблокирующих сокетах.

Запуск каждого модуля:

**server** 

`java -jar server/target/server-1.0-SNAPSHOT.jar [-p PORT] [-h HOST]`

**client** 

`java -jar client/target/client-1.0-SNAPSHOT.jar [-p PORT] [-h HOST] [-u USERNAME]`

**nio-server** 

`java -jar nio-server/target/nio-server-1.0-SNAPSHOT.jar [-p PORT] [-h HOST]`

**nio-client** 

`java -jar nio-client/target/nio-client-1.0-SNAPSHOT.jar [-p PORT] [-h HOST]`

Описание опций:
 - *-h* -- идентификатор сервера в виде IP адреса (по смыслу должен быть адрес хоста). По умолчанию *localhost*
 - *-p* -- порт, который следует прослушивать
 - *-u* -- имя пользователя
 
## Протокол
Имеется один общий тип пакета, он отправляется от клиента к серверу и от сервера клиенту.   
В пунктах `a` и `b` структура пакета не меняется
Формат пакета:
 
Name length | Name | Message length| Message | End
--|--|--|--|--

`Name length` и `Message length` занимают по 1 байту, то есть максимальная длина сообщения и имени - 127 символов.
Флаг `End` показывает последние ли сообщение из общего потока.
Данный влаг нужен для того, чтобы передавать по частям большое сообщение от пользователя.
`Name` и `Message` передаются в формате `utf-8`.
Время сообщения возлагается на клиентов, по причине того, что у пользователей могут быть разные часовые пояса.

## Реализация 
Реализация обоих пунктов похожа, за исключением самих сокетов. Обычные сокеты были взяты из пакета  `java.net`, а неблокирующие взяты из `java.nio`.

**СЕРВЕР**

При запуске сервера считываются аргументы командной строки. На сервере логируется информация о подключении, также логиуруются сообщения пользователей и их имена просто для красоты, в реальном серверере такого не должно быть.
Сохраняются только имена пользователей для того, чтобы при попытке отправить сообщение не от своего лица, вставлялось имя реального отправителя..

**КЛИЕНТ**

При запуске клиента также считываются аргументы командной строки, у пользователя запрашивается имя. Имя отправляется вместе с каждым сообщением, так как имена пользователей не хранятся на сервере. Выйти из программы можно с помощью команды "quit". Если запустить клиент, а сервер в этот момент не будет работать, выведется соответсвтующее сообщение и программа завершит работу.

