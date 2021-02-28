def my_response(oid):
    res = '|'.join(oid.split('.'))
    return octet_string('response: {}'.format(res))


DATA = {
    '1.3.6.1.4.1.1.1.0': integer(2021),
    '1.3.6.1.4.1.1.3.0': octet_string('Test string'),
    '1.3.6.1.4.1.1.4.0': null(),
    '1.3.6.1.4.1.1.5.0': object_identifier('1.3.6.7.8.9'),
}
