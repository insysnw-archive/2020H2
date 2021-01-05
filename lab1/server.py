#!/usr/bin/env python3

import argparse

parser = argparse.ArgumentParser(description='TCP chat server.')
parser.add_argument('--host', default='0.0.0.0', help='address to bind to')
parser.add_argument('--port', type=int, default = 9000, help='server port number')
parser.add_argument('--max-username-size', type=int, default = 25, help='maximum user name size')
parser.add_argument('--max-message-size', type=int, default = 3000, help='maximum message size')
args = parser.parse_args()

import asyncio
import socket
import time
import logging
import io

logging.basicConfig(level=logging.INFO)

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind((args.host, args.port))

class ClientError(Exception):
    pass

async def read_string(reader, maximum):
    data = await reader.readexactly(4)
    size = int.from_bytes(data, byteorder='big', signed=False)
    if size > maximum:
        raise ClientError(f"string size is too big ({size} > {maximum})")
    data = await reader.readexactly(size)
    return data.decode("utf-8")

def encode_string(output, string):
    data = string.encode('utf-8')
    size = len(data)
    size_data = size.to_bytes(4, byteorder='big', signed=False)
    output.write(size_data)
    output.write(data)

messages = asyncio.Lock()
consumers = []

async def handle_connection(reader, writer):
    try:
        address = writer.get_extra_info('peername')
        logging.info('%s connected', address)
        # handshake: receive user name and connect to chat room
        username = await read_string(reader, args.max_username_size)
        logging.info('%s connected as "%s"', address, username)
        consumers.append(writer)
        # operation
        try:
            while True:
                message = await read_string(reader, args.max_message_size)
                logging.info('received from [%s]: %s', username, message)
                ## prepare message
                output = io.BytesIO()
                # write time in seconds from epoch
                epoch_time = int(time.time())
                epoch_time_bytes = epoch_time.to_bytes(8, byteorder='big', signed=False)
                output.write(epoch_time_bytes)
                # write username
                encode_string(output, username)
                # write message
                encode_string(output, message)
                # send everyone the same encoded message
                output.seek(0)
                encoded_message = output.read()
                async with messages:
                    for consumer in consumers:
                        try:
                            consumer.write(encoded_message)
                            await consumer.drain()
                        except Exception as ex:
                            logging.error(ex)
        finally:
            consumers.remove(writer)
    except Exception as ex:
        logging.error(ex)
    finally:
        writer.close()
        logging.info('disconnected %s', address)

async def main():
    server = await asyncio.start_server(handle_connection, sock=server_socket)

    address = server_socket.getsockname()
    logging.info('binding to %s', address)

    async with server:
        await server.serve_forever()

try:
    asyncio.run(main())
except KeyboardInterrupt:
    pass
finally:
    server_socket.close()
    logging.info("socket closed")
