from flask import Flask, json

from src.consts import SWAGGER_URL, API_URL
from .api import blueprint_fast, blueprint_slow, blueprint_swagger_ui
from .spec import *

app = Flask(__name__)
openapi = Spec()


@app.route(API_URL)
def create_swagger_spec():
    return json.dumps(openapi.spec())


def config():
    # Кофигурация сервиса
    app.register_blueprint(blueprint_swagger_ui, url_prefix=SWAGGER_URL)
    app.register_blueprint(blueprint_fast)
    app.register_blueprint(blueprint_slow)

    # Кофигурация OpenApi
    openapi.add_tag({"name": "calculator", "description": "Математические дейтсвия"})
    openapi.add_schema(ArithmeticErrorSchema)
    openapi.add_schema(TimeoutErrorSchema)
    openapi.add_schema(FastOperationSchema)
    openapi.add_schema(FormErrorSchema)
    openapi.add_schema(PolingSchema)
    openapi.add_schema(OperationResultSchema)
    openapi.add_schema(SlowOperationSchema)
    openapi.load_docstrings(app)
    openapi.write_yaml_file()


if __name__ == '__main__':
    config()
    app.run(host='0.0.0.0')
