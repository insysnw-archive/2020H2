import select
import socket

address = (socket.gethostname(), 8686)

max_connections = 20

inputs = list()
outs = list()


def get_socket():

    # Создаем сокет
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setblocking(0)

    # Биндим сервер на нужный адрес и порт
    server.bind(address)

    # Установливаем максимальное количество подключений
    server.listen(max_connections)

    return server


def handle_connections(conn, server):
    
    for resource in conn:

        # Обработка нового подключения
        if resource is server:
            connection, client_address = resource.accept()
            connection.setblocking(0)
            inputs.append(connection)
            print("New connection from {address}".format(address=client_address))
            
        else:
            data = ""
            try:
                data = resource.recv(1024)

            except ConnectionResetError:
                pass

            if data:
                # Вывод полученных данных на консоль
                print(data.decode('utf-8'))

                if resource not in outs:
                    outs.append(resource)

                # Отправка данных всем клиентам
                for i in outs:
                    i.sendall(data)
                    

            else:
                # Очистка данных о ресурсе
                clear_resource(resource)


def clear_resource(resource):

    if resource in outs:
        outs.remove(resource)
    if resource in inputs:
        inputs.remove(resource)
    resource.close()

    print('Closing connection ' + str(resource))



if __name__ == '__main__':

    server_socket = get_socket()
    inputs.append(server_socket)

    print("Server is running, please, press ctrl+c to stop")
    try:
        while True:
            conn, writables, exception = select.select(inputs, outs, inputs)
            handle_connections(conn, server_socket)

    except KeyboardInterrupt:
        clear_resource(server_socket)
        print("Server stopped!")
