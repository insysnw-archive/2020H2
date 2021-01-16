import time

OK = 0
TIMEOUT = 1
ERROR = 2


# факториал с проверкой на timeout
def factorial(n, timeout) -> (int, float, str):
    float_n = n
    n = int(float_n)
    if not n == float_n:
        return ERROR, -1, "Need int, not float"
    time.sleep(2)
    result = 1
    try:
        for i in range(2, n + 1):
            if time.time() >= timeout:
                return TIMEOUT, result, ""
            else:
                result = result * i
        return OK, result, ""
    except Exception as e:
        return ERROR, result, str(e)


# квадратный корень с проверкой на timeout
def sqrt(n, timeout) -> (int, float, str):
    time.sleep(2)
    try:
        result = n ** (1 / 2)
        if time.time() >= timeout:
            return TIMEOUT, result, ""
        else:
            return OK, result, ""
    except Exception as e:
        return ERROR, -1, str(e)


def sum(a, b) -> (int, float, str):
    try:
        return OK, a + b, ""
    except Exception as e:
        return ERROR, -1, str(e)


def sub(a, b) -> (int, float, str):
    try:
        return OK, a - b, ""
    except Exception as e:
        return ERROR, -1, str(e)


def mul(a, b) -> (int, float, str):
    try:
        return OK, a * b, ""
    except Exception as e:
        return ERROR, -1, str(e)


def div(a, b) -> (int, float, str):
    try:
        return OK, a / b, ""
    except Exception as e:
        return ERROR, -1, str(e)
