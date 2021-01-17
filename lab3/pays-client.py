#!/usr/bin/env python3

import argparse

parser = argparse.ArgumentParser(description='Pays client')
parser.add_argument('--host', default='localhost', help='server hostname')
parser.add_argument('--port', type=int, default=4242, help='server TCP port')
args = parser.parse_args()

import shared
import pays
import py_cui
import os

exit_error = None

def showerror(function):
    def decorated(*args, **kwargs):
        global exit_error
        try:
            function(*args, **kwargs)
        except Exception as e:
            root.show_message_popup('Error!', str(e))
            if not isinstance(e, shared.ClientError) and not isinstance(e, ValueError):
                exit_error = e
                root.stop()
    return decorated

client = shared.Client(args.host, args.port)

root = py_cui.PyCUI(8, 4)

## News View

my_wallet_box = root.add_text_box('My Wallet', 0, 0, 1, 2)
my_money_box = root.add_text_box('Money', 1, 0, 1, 2)
wallets_menu = root.add_scroll_menu('Wallets', 0, 2, 7, 2)

wallet = None

my_wallet_box.set_selectable(False)
my_wallet_box.set_text('<not authorised>')
my_money_box.set_selectable(False)
my_money_box.set_text('<not authorised>')

all_wallets = []

def update_wallet():
    my_wallet_box.set_text(str(wallet.id))
    my_money_box.set_text(str(wallet.money))

@showerror
def on_refresh_wallets():
    global all_wallets
    packet = client.request(pays.WalletsRequest())
    all_wallets = packet.wallets
    wallets_menu.clear()
    wallets_menu.add_item_list(['#' + str(x) for x in all_wallets])
    if wallet is not None:
        wallet.money = packet.money
        update_wallet()

@showerror
def on_login_submit(form):
    global wallet
    wallet_id = int(form['Wallet'])
    packet = client.request(pays.LoginRequest(wallet_id, form['Password']))
    wallet = pays.Wallet(wallet_id, '', packet.money)
    update_wallet()

@showerror
def on_login():
    root.show_form_popup(
        'Login',
        ['Wallet', 'Password'],
        passwd_fields=['Password'],
        required=['Wallet'],
        callback=on_login_submit
    )

@showerror
def on_register_submit(form):
    global wallet
    packet = client.request(pays.RegisterRequest(form['Password']))
    wallet = pays.Wallet(packet.wallet, '', packet.money)
    update_wallet()

@showerror
def on_register():
    root.show_form_popup(
        'Register',
        ['Password'],
        passwd_fields=['Password'],
        callback=on_register_submit
    )

def get_sellected_wallet():
    dest = wallets_menu.get_selected_item_index()
    if dest is None:
        return None
    return all_wallets[dest]

@showerror
def on_transaction(form):
    dest = get_sellected_wallet()
    money = int(form['Money'])
    if money < 0:
        raise shared.ClientError('you cannot transfer negative amount of money')
    client.request(pays.TransactionRequest(dest, money))
    wallet.money -= money
    update_wallet()

@showerror
def on_init_transaction():
    dest = get_sellected_wallet()
    if dest is None:
        return
    if wallet is not None and wallet.id == dest:
        raise shared.ClientError("you cannot transfer money to yourself")
    root.show_form_popup(
        f"Transfer to Wallet: #{dest}",
        ['Money'],
        callback=on_transaction
    )

wallets_menu.add_key_command(py_cui.keys.KEY_ENTER, on_init_transaction)

root.add_button('Refresh', 7, 2, 1, 2, command=on_refresh_wallets)
root.add_button('Login', 7, 0, command=on_login)
root.add_button('Register', 7, 1, command=on_register)

try:
    client.start()
    on_refresh_wallets()
    root.set_title('Pays client')
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
