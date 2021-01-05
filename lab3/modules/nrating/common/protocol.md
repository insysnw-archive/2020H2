**Содержание**

[[_TOC_]]

# Опции

|       Option       | OptionId |   Value Type    |          Description          |
| ------------------ | -------- | --------------- | ----------------------------- |
| TopicName          | 0x01     | `VarString`     | Название темы голосования     |
| AlternativeName    | 0x02     | `VarString`     | Вариант для выбора            |
| ErrorMessage       | 0x03     | `VarString`     | Причина невыполнения          |
| TopicList          | 0x04     | `VarList<Topic>`| Информация о теме голосования |
| TopicStatus        | 0x05     | `TopicStatus`   | Информация о статусе голосования |

# Типы данных

## Topic
Информация о теме голосования.
Включает в себя:
* `id: VarInt`
* `name: VarString`

## TopicStatus
Информация о статусе голосования.
Включает в себя:
* `topicData: Topic`
* `isOpen: Bool`
* `rating: VarList<RatingItem>`

## RatingItem
Строчка в строке рейтинга
Включает в себя:
* `altId: VarInt`
* `altName: VarString`
* `votesAbs: Int`
* `votesRel: Double`

## Bool
`Byte`. 0 = false, все остальное = true

# Сообщения

|  Message Type  | Code |
| -------------- | ---- |
| QUERY          | 0x31 |
| QUERY_RESPONSE | 0x32 |
| DISCONNECT     | 0x33 |

## Server-bound сообщения
Сообщения, посылаемые клиентом на сервер
### Query
Поля тела сообщения:

| Method | Topic  | Alternative |
| ------ | ------ | ----------- |
|  Byte  | VarInt | VarInt      |

Список методов:

| Method | Code |
| ------ | ---- |
| GET    | 0x01 |
| ADD    | 0x02 |
| DEL    | 0x03 |
| OPEN   | 0x04 |
| CLOSE  | 0x05 |
| VOTE   | 0x06 |

* #### Get Topic List
  Запрос на получение списка имен и идентификаторов всех тем

  Тело:

| Method | Topic | Alternative |
| ------ | ----- | ----------- |
| GET    | 0     | 0           |

* #### Get Topic Info
  Запрос на получение информации о теме и статуса голосования

  Тело:

| Method |  Topic  | Alternative |
| ------ | ------- | ----------- |
| GET    | Id темы | 0           |

* #### Add Topic
  Запрос на добавление новой темы для голосования
  Тело:

| Method | Topic |      Alternative       |
| ------ | ----- | ---------------------- |
| ADD    | 0     | Число вариантов выбора |


* #### Remove Topic
  Запрос на удаление темы для голосования
  Тело:

| Method |  Topic  | Alternative |
| ------ | ------- | ----------- |
| DEL    | Id темы |      0      |

* #### Add Alternative
  Запрос на добавление нового варианта для голосования в заданной теме
  Тело:

| Method |  Topic  | Alternative |
| ------ | ------- | ----------- |
| ADD    | Id темы |      0      |

Опции:

|     Option      |          Value          |
| --------------- | ----------------------- |
| AlternativeName | Вариант для голосования |

*Примечание:*
Если голосование уже началось, в него нельзя добавлять новые варианты


* #### Remove Alternative
  Запрос на удаление варианта для голосования из заданной темы
  Тело:
  
| Method |  Topic  | Alternative |
| ------ | ------- | ----------- |
| DEL    | Id темы | Id варианта |

*Примечание:*
Если голосование уже началось, из него нельзя удалять варианты


* #### Open Vote
  Запрос на открытие голосования по теме
  Тело:

| Method |  Topic  | Alternative |
| ------ | ------- | ----------- |
| OPEN   | Id темы |      0      |

* #### Close Vote
  Запрос на закрытие голосования по теме
  Тело:

| Method |  Topic  | Alternative |
| ------ | ------- | ----------- |
| CLOSE  | Id темы |      0      |

* #### Vote
  Отдать голос за определенный вариант
  Тело:

| Method |  Topic  | Alternative |
| ------ | ------- | ----------- |
| VOTE   | Id темы | Id варианта |

## Client-bound сообщения
Сообщения, посылаемые сервером клиенту

### Query Response
Поля тела сообщения:

| Method | Status | Topic  | Alternative |
| ------ | ------ | ------ | ----------- |
|  Byte  |  Byte  | VarInt | VarInt      |

Возможные значения Status (Byte):

| Status | Code |
| ------ | ---- |
| OK     | 0x01 |
| FAILED | 0x02 |

Значения методов совпадают с методами из запроса.

В случае, если запрос не был выполнен, к сообщению добавляется опция ErrorMessage

|    Option    |         Value        |
| ------------ | -------------------- |
| ErrorMessage | Причина невыполнения |


* #### Get Topic List Response
  Ответ на запрос Get Topic List

  Тело:

| Method | Status | Topic | Alternative |
| ------ | ------ | ----- | ----------- |
| GET    | OK     | 0     | 0           |

Опции:

|  Option   |     Value     |
| --------- | ------------- |
| TopicList |  Список тем   |

Если тем нет, отправляется пустой список

* #### Get Topic Info Response
  Ответ на запрос Get Topic Info

  Тело:

| Method |    Status   |  Topic  | Alternative |
| ------ | ----------- | ------- | ----------- |
| GET    | OK / FAILED | Id темы | 0           |

Опции:

|    Option    |              Value             |
| ------------ | ------------------------------ |
| TopicStatus    | Информация о запашиваемой теме |

* #### Add Topic Response
  Ответ на запрос Add Topic
  Тело:

| Method |   Status    |       Topic       | Alternative |
| ------ | ----------- | ----------------- | ----------- |
| ADD    | OK / FAILED | Id созданной темы | 0           |

* #### Remove Topic Response
  Ответ на запрос Remove Topic
  Тело:

| Method |   Status    |               Topic            | Alternative |
| ------ | ----------- | ------------------------------ | ----------- |
| DEL    | OK / FAILED | Id темы из клиентского запроса | 0           |

* #### Add Alternative Response
  Ответ на запрос Add Alternative
  Тело:

| Method |   Status    |  Topic  |           Alternative          |
| ------ | ----------- | ------- | ------------------------------ |
| ADD    | OK / FAILED | Id темы | Id созданного варианта выбора  |


* #### Remove Alternative
  Ответ на запрос Remove Alternative
  Тело:

| Method |   Status    |               Topic            | Alternative |
| ------ | ----------- | ------------------------------ | ----------- |
| DEL    | OK / FAILED | Id темы из клиентского запроса | Id варианта из клиентского запроса  |


* #### Open Vote

| Method |   Status    |  Topic  | Alternative |
| ------ | ----------- | ------- | ----------- |
| OPEN   | OK / FAILED | Id темы | 0           |

* #### Close Vote

| Method |   Status    |  Topic  | Alternative |
| ------ | ----------- | ------- | ----------- |
| CLOSE  | OK / FAILED | Id темы | 0           |

* #### Vote

| Method |   Status    |  Topic  | Alternative |
| ------ | ----------- | ------- | ----------- |
| VOTE   | OK / FAILED | Id темы | 0           |

### Disconnect
Сообщение не имеет тела. Посылается клиенту перед тем, как принудительно разорвать с ним соединение.
Заполняется опция ErrorMessage с объяснением причины отключения.

|    Option    |       Value        |
| ------------ | ------------------ |
| ErrorMessage | Причина отключения |

