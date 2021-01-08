# Лабораторная работа 2 пункт k
В табличке указан неверный вариант! 
Вов ремя выбора варианта я фиксировал за собой `dns клиент` и `dhcp сервер`!

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

Запустить сервер можно тут же на месте, запустив после сборки команду:

    ./mvnw exec:java -Dexec.args='[-t TIMEOUT] [-r RETRIES] [-mx|-ns] @server name'

Либо:

```
java -jar target/2020H2-1.0-SNAPSHOT.jar [-t TIMEOUT] [-r RETRIES] [-mx|-ns] @server name
```

Описание опций:
 - *-t* -- таймаут
 - *-r* -- количество попыток
 - *-mx | -ns* -- флаги-mx или-ns (необязательно) указывают, следует ли отправлять запрос MX (почтовый сервер) или NS (сервер имен). Если ни один из них не задан, то клиент отправит запрос типа А (IP-адрес).   
 - *@* -- обязательная опция; через `@` передается адрес DNS сервера (IPv4-адрес в формате a.b.c.d.), далее ожидается доменное имя, адрес которого необходимо узнать
 
## Инструкция по использованию 
Для запуска необходимо скачать jdk или jre Запуск можно производить из командной строки или из IDE. Для второго варианта запуск зависит от конкретной IDE, в случае первого варината необходимо:

Зайти в папку с проектом  
В командную строку ввести команду:  
```
java -jar out/artifacts/DNS_jar/2020H2.jar <аргументы командной строки>
```

## Демонстрация работы
DNS клиент реализует протокол, описанный в документе [RFC1035](https://tools.ietf.org/html/rfc1035).   
Клиент работает с записями типа: A (IP-адреса), MX (почтовый сервер) и NS (сервер имен).
__ПРИМЕР 1:__  

**Запрос**   
```
java -jar out/artifacts/DNS_jar/2020H2.jar –t 10 –r 2 –mx @8.8.8.8 vk.com
```

**Ответ**  
```shell script
DNSClient sending request for vk.com
Server: 8.8.8.8
Request type: A
Response received after 0.023 seconds (0 retries)

***Answer Section (6 answerRecords)***
IP      87.240.190.78   26      nonauth
IP      93.186.225.208  26      nonauth
IP      87.240.139.194  26      nonauth
IP      87.240.137.158  26      nonauth
IP      87.240.190.67   26      nonauth
IP      87.240.190.72   26      nonauth
```

__ПРИМЕР 2:__  

**Запрос**   
```
java -jar out/artifacts/DNS_jar/2020H2.jar @8.8.4.4 yandex.ru
```

**Ответ**  
```shell script
DNSClient sending request for yandex.ru
Server: 8.8.4.4
Request type: A
Response received after 0.019 seconds (0 retries)

***Answer Section (4 answerRecords)***
IP      77.88.55.70     4       nonauth
IP      5.255.255.70    4       nonauth
IP      5.255.255.60    4       nonauth
IP      77.88.55.66     4       nonauth
```