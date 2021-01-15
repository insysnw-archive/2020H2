#!/usr/bin/env python3

import argparse

parser = argparse.ArgumentParser(description='News server')
parser.add_argument('--host', default='localhost', help='server binding address')
parser.add_argument('--port', type=int, default=6969, help='server TCP port')
args = parser.parse_args()

import asyncio
import logging
import socket
import shared
import news

logging.basicConfig(level=logging.INFO)

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind((args.host, args.port))

posts_lock = asyncio.Lock()
posts_by_id = {}
posts_by_theme = {}
posts_counter = 0

def posts_synch(function):
    async def decorated(*args, **kwargs):
        async with posts_lock:
            return function(*args, **kwargs)
    return decorated

@posts_synch
def get_post(post_id: int) -> news.Post:
    result = posts_by_id.get(post_id, None)
    if result == None:
        raise shared.ClientError(f"no post with id {post_id}")
    else:
        return result

@posts_synch
def add_post(post: news.Post) -> int:
    global posts_counter
    post_id = posts_counter
    posts_counter += 1
    posts_by_id[post_id] = post
    post.id = post_id
    for theme in post.themes:
        post_list = posts_by_theme.get(theme, None)
        if post_list == None:
            post_list = []
            posts_by_theme[theme] = post_list
        post_list.append(post)
    return post_id

def clone(a) -> list:
    return [x for x in a]

@posts_synch
def get_posts_by_theme(theme):
    themed = posts_by_theme.get(theme, None)
    if themed == None:
        return []
    else:
        return [(post.id, post.title) for post in themed]

@posts_synch
def get_all_posts():
    return [(post.id, post.title) for post in posts_by_id.values()]

@posts_synch
def get_all_themes():
    return clone(posts_by_theme.keys())

async def handle_connection(reader, writer):
    try:
        address = writer.get_extra_info('peername')
        logging.info('%s connected', address)
        respondent = shared.Respondent(reader, writer)

        @respondent.reply(news.GetAllThemesPacket)
        async def on_get_themes(packet):
            return news.AllThemesPacket(await get_all_themes())
        
        @respondent.reply(news.GetNewsPacket)
        async def on_get_news(packet):
            if packet.theme == "":
                return news.NewsPacket(await get_all_posts())
            else:
                return news.NewsPacket(await get_posts_by_theme(packet.theme))

        @respondent.reply(news.GetPostPacket)
        async def on_get_post(packet):
            return news.PostPacket(packet.post_id, await get_post(packet.post_id))

        @respondent.reply(news.AddPostPacket)
        async def on_add_post(packet):
            return news.PostIdPacket(await add_post(packet.post))

        await respondent.run()

    except Exception as ex:
        logging.error(ex)
    finally:
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
