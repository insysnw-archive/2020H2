from flask_swagger_ui import get_swaggerui_blueprint

from src.consts import SWAGGER_URL, API_URL

blueprint_swagger_ui = get_swaggerui_blueprint(
    SWAGGER_URL,
    API_URL,
    config={
        'app_name': 'My App'
    }
)
