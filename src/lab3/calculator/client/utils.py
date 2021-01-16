import re


def __is_digit(inp: str):
    return not re.match(r'^-?\d+(?:\.\d+)?$', inp) is None


def __clear_str(inp: str):
    return re.sub(r'[\n\t\r\s]*', '', inp)


def validate_argument(input_string: str):
    input_string = input_string.replace(" ", "")
    if __is_digit(input_string):
        return float(input_string)
    else:
        return None


def validate_operation(input_string: str):
    input_string = __clear_str(input_string)

    if input_string == "+":
        return 0
    elif input_string == '-':
        return 1
    elif input_string == '*':
        return 2
    elif input_string == '/':
        return 3
    elif input_string == 'sqrt':
        return 4
    elif input_string == '!':
        return 5
    else:
        return -1


def parse(inp: str):
    inp = __clear_str(inp)
    fast_ops = re.match(r'^(-?\d+(?:\.\d+)?)([\\+-\\*/])(-?\d+(?:\.\d+)?)$', inp)
    fact_op = re.match(r'^(\d+(?:\.\d+)?)\!(?:t=(\d+(?:\.\d+)?))?$', inp)
    sqrt_op = re.match(r'^sqrt[\\(](\d+(?:\.\d+)?)[\\)](?:t=(\d+(?:\.\d+)?))?$', inp)

    if fast_ops:
        arg1 = float(fast_ops.group(1))
        op = fast_ops.group(2)
        arg2 = float(fast_ops.group(3))
        op_code = validate_operation(op)
        return arg1, arg2, op_code
    elif sqrt_op or fact_op:
        reg = sqrt_op if sqrt_op else fact_op
        op_code = 4 if sqrt_op else 5
        arg1 = float(reg.group(1))
        timeout = 0.1 if reg.group(2) is None else float(reg.group(2))
        return arg1, timeout, op_code
    else:
        return None, None, None
