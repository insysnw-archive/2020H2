import math

from flask import Blueprint

blueprint_slow = Blueprint(name="slow", import_name=__name__)

operation = {
    1: lambda x: math.factorial(x),
    2: lambda x: math.sqrt(x),
}


@blueprint_slow.route("/slow", methods=['GET'])
def calc():
    """
        ---
           get:
             summary: Произовдит медленные операции
             parameters:
               - in: query
                 schema: SlowOperationSchema
             responses:
               '200':
                 description: Идентификатор результата
                 content:
                   application/json:
                     schema: PolingSchema
               '400':
                 description: Неверная форма отправки
                 content:
                   application/json:
                     schema: FormErrorSchema
             tags:
               - calculator
           """
    pass


@blueprint_slow.route("/slow/<int:result_id>", methods=['GET'])
def result(result_id):
    """
        ---
           get:
             summary: Возваращет результат операции, если был вычислен
             parameters:
               - in: path
                 schema: PolingSchema
             responses:
               '200':
                 description: Идентификатор результата
                 content:
                   application/json:
                     schema: OperationResultSchema
               '204':
                 description: Ответ еще не получен
               '400':
                 description: Неверная форма отправки
                 content:
                   application/json:
                     schema: FormErrorSchema
               '408':
                 description: Время ожидания истекло
                 content:
                   application/json:
                     schema: TimeoutErrorSchema
             tags:
               - calculator
           """
    pass
