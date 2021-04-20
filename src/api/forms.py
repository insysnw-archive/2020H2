from flask_wtf import Form
from wtforms import FloatField, IntegerField
from wtforms.validators import DataRequired


class FastOperationForm(Form):
    operand_1 = FloatField("operand_1", validators=[DataRequired()])
    operand_2 = FloatField("operand_2", validators=[DataRequired()])
    operand_type = IntegerField("operand_type", validators=[DataRequired()])


class SlowOperationForm(Form):
    operand_1 = FloatField("operand_1", validators=[DataRequired()])
    operand_type = IntegerField("operand_type", validators=[DataRequired()])
    timeout = IntegerField("timeout", default=5)


class PolingForm(Form):
    poling_id = IntegerField("poling_id", validators=[DataRequired()])
