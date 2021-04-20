from apispec import APISpec
from apispec.ext.marshmallow import MarshmallowPlugin
from apispec_webframeworks.flask import FlaskPlugin

from src.consts import DOCS_FILENAME


class Spec:
    def __init__(self, title="Calculator", version="1.0.0", openapi_version="3.0.3", schemas=None, tags=None):
        """
        Формируем объект APISpec.
        :param app: объект Flask приложения
        """
        self._spec = APISpec(
            title=title,
            version=version,
            openapi_version=openapi_version,
            plugins=[FlaskPlugin(), MarshmallowPlugin()],
        )

        if schemas:
            for schema in schemas:
                self.add_schema(schema)

        if tags:
            for tag in tags:
                self.add_tag(tag)

    def add_schema(self, schema):
        """
        Добавляет схему.
        :param schema: схема
        """
        schema_name = schema.__name__
        print(f"Добавляем схему: {schema_name}")
        self._spec.components.schema(schema_name, schema=schema)
        return self

    def add_tag(self, tag: dict):
        """
        Добавляет тег.
        :param tag: словарь
        """
        print(f"Добавляем тэг: {tag['name']}")
        self._spec.tag(tag)
        return self

    def load_docstrings(self, app):
        """ Загружаем описание API.
        :param app: экземпляр Flask приложения, откуда берем описание функций
        """
        with app.test_request_context():
            for fn_name in app.view_functions:
                if fn_name == 'static':
                    continue
                print(f"Загружаем описание функций: {fn_name}")
                view_fn = app.view_functions[fn_name]
                self._spec.path(view=view_fn)
        return self

    def write_yaml_file(self):
        """ Экспортируем объект APISpec в YAML файл."""
        with open(DOCS_FILENAME, 'w') as file:
            file.write(self._spec.to_yaml())
        print(f'Сохранили документацию в {DOCS_FILENAME}')
        return self

    def spec(self):
        return self._spec.to_dict()
