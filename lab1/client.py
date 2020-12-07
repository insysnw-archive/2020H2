#!/usr/bin/env python3

import argparse

parser = argparse.ArgumentParser(description='TCP chat client.')
parser.add_argument('--host', default='localhost', help='address to connect')
parser.add_argument('--port', type=int, default = 9000, help='server port number')
parser.add_argument('username', help='your name in chat')
args = parser.parse_args()

import asyncio
import socket
import sys
import time
import os
from threading import Thread

client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect((args.host, args.port))

messages = None

def read_stdin(loop):
    for line in sys.stdin: # blocking code, needs separated thread
        asyncio.run_coroutine_threadsafe(messages.put(line.rstrip()), loop)
    os._exit(0) # input stream exhausted, exit chat

def write_string(writer, string):
    data = string.encode('utf-8')
    size = len(data)
    size_data = size.to_bytes(4, byteorder='big', signed=False)
    writer.write(size_data)
    writer.write(data)

async def read_string(reader):
    data = await reader.readexactly(4)
    size = int.from_bytes(data, byteorder='big', signed=False)
    data = await reader.readexactly(size)
    return data.decode("utf-8")

async def read_messages(reader):
    while True:
        try:
            data = await reader.readexactly(8)
            epoch_seconds = int.from_bytes(data, byteorder='big', signed=False)
            time_string = time.strftime('%H:%M', time.localtime(epoch_seconds))
            user = await read_string(reader)
            message = await read_string(reader)
            print(f'<{time_string}> [{user}] {message}')
        except Exception as ex:
            print(ex)
            os._exit(1)

async def write_messages(writer):
    while True:
        message = await messages.get()
        write_string(writer, message)
        await writer.drain()

async def main():
    global messages
    messages = asyncio.Queue()
    loop = asyncio.get_running_loop()
    thread = Thread(target=read_stdin, args=(loop,), daemon=True)
    thread.start()
    reader, writer = await asyncio.open_connection(sock=client_socket)
    # send our user name to the chat server
    write_string(writer, args.username)
    await writer.drain()
    read_task = asyncio.create_task(read_messages(reader))
    await write_messages(writer)
    await read_task

asyncio.run(main())
