import sys
import struct
import os
from threading import Thread, Event
import socket

host = '0.0.0.0'
port = 9090

# start
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind((host, port))
server.listen()

clients = []
nicknames = {}