from marshmallow import Schema, fields


class FastOperationSchema(Schema):
    operand_1 = fields.Float(description="Первый операнд", required=True, example=5.2)
    operand_2 = fields.Float(description="Второй операд", required=True, example=5.2)
    operand_type = fields.Int(description="Тип операнда ( 1: + ;  2: - ; 3: * ;  4: / )", required=True, example=1)


class SlowOperationSchema(Schema):
    operand_1 = fields.Float(description="Первый операнд", required=True, example=5)
    operand_type = fields.Int(description="Тип операнда ( 1: ! ;  2: sqtr )", required=True, example=1)
    timeout = fields.Int(description="Пороговое время ожидания (в секундах)", required=False, example=5)


class PolingSchema(Schema):
    result_id = fields.Int(description="Идентификатор операции", required=True, example=1123123)


class OperationResultSchema(Schema):
    result = fields.Float(description="Результат операции", required=True, example=10.4)


class ArithmeticErrorSchema(Schema):
    error = fields.String(description="Ошибка вычисления", required=True, example='Деление на 0')


class FormErrorSchema(Schema):
    error = fields.String(description="Форма содержит ошибки", required=True, example='operand_1 не является числом')


class TimeoutErrorSchema(Schema):
    error = fields.String(description="Timeout", required=True, example='Вычисление результата превысило предел')
