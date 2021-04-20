from flask import Blueprint, current_app, json

from src.api.forms import FastOperationForm

blueprint_fast = Blueprint(name="fast", import_name=__name__)

operation = {
    1: lambda x, y: x + y,
    2: lambda x, y: x - y,
    3: lambda x, y: x / y,
    4: lambda x, y: x * y,
}


@blueprint_fast.route("/fast", methods=['GET'])
def calc():
    """
       ---
       get:
         summary: Произовдит быстрые операции над двумя операндами
         parameters:
           - in: query
             schema: FastOperationSchema
         responses:
           '200':
             description: Результат операции
             content:
               application/json:
                 schema: OperationResultSchema
           '400':
             description: Неверная форма отправки
             content:
               application/json:
                 schema: FormErrorSchema
           '406':
             description: Невозможно получить результат
             content:
               application/json:
                 schema: ArithmeticErrorSchema
         tags:
           - calculator
       """
    form = FastOperationForm()
    if form.validate_on_submit():
        if form.operand_type in range(1, 5):
            if form.operand_2 == 0 and form.operand_type == 3:
                return current_app.response_class(
                    response=json.dumps(dict(error="Деление на 0")),
                    status=406,
                    mimetype='application/json'
                )
            result = operation.get(form.operand_type)(form.operand_1, form.operand_2)
            return current_app.response_class(
                response=json.dumps(dict(result=result)),
                status=200,
                mimetype='application/json'
            )
        else:
            return current_app.response_class(
                response=json.dumps(dict(error="Тип операции не поддерживается")),
                status=400,
                mimetype='application/json'
            )
    else:
        return current_app.response_class(
            response=json.dumps(dict(error="Неверная форма запроса")),
            status=400,
            mimetype='application/json'
        )
