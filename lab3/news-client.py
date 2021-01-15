#!/usr/bin/env python3

import argparse

parser = argparse.ArgumentParser(description='News client')
parser.add_argument('--host', default='localhost', help='server hostname')
parser.add_argument('--port', type=int, default=6969, help='server TCP port')
args = parser.parse_args()

import shared, news, py_cui, os

client = shared.Client(args.host, args.port)

root = py_cui.PyCUI(6, 3)
news_view = root.create_new_widget_set(8, 4)
new_post_view = root.create_new_widget_set(8, 4)

## News View

themes_menu = news_view.add_scroll_menu('Categories', 0, 0, 6)
news_menu = news_view.add_scroll_menu('News', 0, 1, 6)
title = news_view.add_text_box('Title', 0, 2, 1, 2)
post_themes = news_view.add_text_box('Categories', 1, 2, 1, 2)
body = news_view.add_text_block('Content', 2, 2, 6, 2)

title.set_selectable(False)
post_themes.set_selectable(False)
body.set_selectable(False)

selected_theme = ''
news_list = []

exit_error = None

def showerror(function):
    def decorated():
        global exit_error
        try:
            function()
        except Exception as e:
            root.show_message_popup('Error!', str(e))
            if not isinstance(e, shared.ClientError):
                exit_error = e
                root.stop()
    return decorated

@showerror
def on_refresh_news():
    global news_list
    response = client.request(news.GetNewsPacket(selected_theme))
    news_list = response.posts
    news_menu.clear()
    news_menu.add_item_list([x[1] for x in news_list])

@showerror
def on_select_theme():
    theme = themes_menu.get()
    if theme == None:
        return
    global selected_theme
    if theme == 'all':
        selected_theme = ''
    else:
        selected_theme = theme
    on_refresh_news()

@showerror
def on_select_post():
    post_n = news_menu.get_selected_item_index()
    if post_n == None:
        return
    post_id = news_list[post_n][0]
    packet = client.request(news.GetPostPacket(post_id))
    title.set_text(packet.post.title)
    post_themes.set_text('; '.join(packet.post.themes))
    body.set_text(packet.post.content)

@showerror
def on_refresh_themes():
    all_themes = client.request(news.GetAllThemesPacket())
    themes_menu.clear()
    themes_menu.add_item_list(['all'] + all_themes.themes)

@showerror
def on_create_post():
    root.apply_widget_set(new_post_view)

themes_menu.add_key_command(py_cui.keys.KEY_ENTER, on_select_theme)
news_menu.add_key_command(py_cui.keys.KEY_ENTER, on_select_post)

news_view.add_button('Refresh', 6, 0, command=on_refresh_themes)
news_view.add_button('Refresh', 6, 1, command=on_refresh_news)
news_view.add_button('Create Post', 7, 0, 1, 2, command=on_create_post)

# New Post View

new_title = new_post_view.add_text_box('Title', 0, 0, 1, 3)
new_post_themes = new_post_view.add_text_box('Categories', 1, 0, 1, 3)
new_body = new_post_view.add_text_block('Content', 2, 0, 6, 4)

@showerror
def clear_new_post():
    new_title.clear()
    new_post_themes.clear()
    new_body.clear()

@showerror
def on_commit_new_post():
    title = new_title.get()
    themes = [x.strip() for x in new_post_themes.get().split(';')]
    if themes == ['']:
        themes = []
    content = new_body.get()
    post = news.Post(title, themes, content)
    post_id = client.request(news.AddPostPacket(post)).post_id
    news_list.append((post_id, title))
    news_menu.add_item(title)
    clear_new_post()
    root.apply_widget_set(news_view)

@showerror
def on_cancel_new_post():
    clear_new_post()
    root.apply_widget_set(news_view)

new_post_view.add_button('Create', 0, 3, command=on_commit_new_post)
new_post_view.add_button('Cancel', 1, 3, command=on_cancel_new_post)

try:
    client.start()
    root.apply_widget_set(news_view)
    on_refresh_themes()
    on_refresh_news()
    root.set_title('News client')
    root.start()
except KeyboardInterrupt:
    pass
except Exception as e:
    print(e)
finally:
    client.client_socket.close()
    if exit_error != None:
        print(exit_error)
    os._exit(0)
