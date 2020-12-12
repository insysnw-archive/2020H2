import os
import socket
import threading
import datetime
from tkinter import *

PORT = 1234
SERVER = socket.gethostbyname(socket.gethostname())
ADDRESS = (SERVER, PORT)
FORMAT = "utf-8"

# create a new client socket
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# connect to the server
client.connect(ADDRESS)


class Chat:
    def __init__(self):
        def on_closing():
            self.window.destroy()
            client.shutdown(socket.SHUT_RDWR)
            client.close()
            quit()

        self.name = ''
        # chat window which is currently hidden
        self.window = Tk()
        self.window.withdraw()

        self.window.protocol("WM_DELETE_WINDOW", on_closing)

        self.chatWindow = Text(self.window, width=20, height=2, bg="#FFFFFF", fg="#000000", padx=5, pady=5)
        self.labelBottom = Label(self.window, bg="#ABB2B9", height=80)
        self.entryMsg = Text(self.labelBottom, bg="#FFFFFF", fg="#000000")
        self.buttonSend = Button(self.labelBottom, text="Send", width=20, bg="#ABB2B9",
                                 command=lambda: self.send_message(self.entryMsg.get("1.0", END)))

        # login window
        self.login = Toplevel()
        self.login.title("Login")
        self.login.resizable(width=False, height=False)
        self.login.configure(width=300, height=100)

        self.login.protocol("WM_DELETE_WINDOW", on_closing)

        self.pls = Label(self.login, text="Please login to continue")
        self.pls.place(anchor=NW, relheight=0.35, relx=0, rely=0)

        self.labelName = Label(self.login, text="Enter your nickname: ")
        self.labelName.place(anchor=W, relheight=0.35, relx=0, rely=0.45)

        # handle username entry (50 characters only)
        def limit_entry(*args):
            value = username.get()
            if len(value) > 50:
                username.set(value[:50])

        username = StringVar()
        username.trace('w', limit_entry)
        self.entryName = Entry(self.login, textvariable=username)
        self.entryName.place(relwidth=0.5, relheight=0.2, relx=0.4, rely=0.4)
        self.entryName.focus()

        # Continue button
        self.go = Button(self.login, text="Continue", command=lambda: self.enter_chat(self.entryName.get()))
        self.go.place(anchor=CENTER, relx=0.5, rely=0.8)

        self.window.mainloop()

    def enter_chat(self, name):
        self.login.destroy()
        self.layout(name)

        # the thread to receive messages
        rcv = threading.Thread(target=self.receive)
        rcv.start()

    # main layout for the chat
    def layout(self, name):
        self.name = name
        self.window.deiconify()
        self.window.title("Chat")
        self.window.resizable(width=False, height=False)
        self.window.configure(width=500, height=600)

        self.chatWindow.place(relheight=0.92, relwidth=0.96, rely=0)

        self.labelBottom.place(relwidth=1, relheigh=0.08, rely=0.92)

        self.entryMsg.place(relwidth=0.74, relheight=1)
        self.entryMsg.focus()

        self.buttonSend.place(relx=0.77, relheight=1, relwidth=0.23)

        scrollbar = Scrollbar(self.window)
        scrollbar.place(relheight=0.92, relx=0.96)
        scrollbar.config(command=self.chatWindow.yview)

        self.chatWindow.config(state=DISABLED)

    def send_message(self, msg):
        self.entryMsg.delete("1.0", END)
        now = datetime.datetime.now()
        message = f"\n\n<{now.strftime('%H:%M')}>[{self.name}] {msg}"
        client.send(message.encode(FORMAT))

    def receive(self):
        while True:
            try:
                message = client.recv(1024).decode(FORMAT)
                if len(message) == 0:
                    break

                # if the messages from the server is NAME send the client's name
                if message == 'NAME':
                    client.send(self.name.encode(FORMAT))
                else:
                    # insert messages to the text box
                    self.chatWindow.config(state=NORMAL)
                    self.chatWindow.insert(END, message)

                    self.chatWindow.config(state=DISABLED)
                    self.chatWindow.see(END)
            except Exception as e:
                print("Client is not receiving: " + str(e))
                os.abort()


# create a GUI class object
chat = Chat()
