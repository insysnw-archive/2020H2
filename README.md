# Лабораторная работа 2 пункт k
В табличке указан неверный вариант! 
Вов ремя выбора варианта я фиксировал за собой `dns клиент` и `dhcp сервер`!

## Инструкция по использованию 
Для запуска необходимо скачать jdk или jre Запуск можно производить из командной строки или из IDE. Для второго варианта запуск зависит от конкретной IDE, в случае первого варината необходимо:

Зайти в папку с проектом  
В командную строку ввести команду:  
`java -jar out/artifacts/DNS_jar/2020H2.jar <аргументы командной строки>`

## Демонстрация работы
DNS клиент реализует протокол, описанный в документе [RFC1035](https://tools.ietf.org/html/rfc1035).

* Аргумент `-t` - таймаут  
* Аргумент `-r` - количество попыток  
* Аргумент `-mx` - тип запроса MX  
* Аргумент `-ns` - тип запроса NS  
* Через `@` передается адрес DNS сервера, далее ожидается доменное имя, адрес которого необходимо узнать

__ПРИМЕР 1:__  

**Запрос**   
`java -jar out/artifacts/DNS_jar/2020H2.jar –t 10 –r 2 –mx @8.8.8.8 vk.com`

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
`java -jar out/artifacts/DNS_jar/2020H2.jar @8.8.4.4 yandex.ru`

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