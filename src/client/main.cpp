//
// Created by Roman Svechnikov on 10.09.2020.
//

#include "Client.h"
#include <thread>

void userInput(Client &client, const std::string &username);

void printMessage(const ChatMessage &message);


int main() {
    std::printf("Please, input your name:\n>");
    std::string name;
    std::getline(std::cin, name);
    Client client{name};
    client.onMessageReceived = [](const ChatMessage &msg) {
        printMessage(msg);
    };


    std::thread inputThread([&] { userInput(client, name); });
    std::printf("Connecting to the TCP server on localhost:4242...\n");
    client.connectTo("127.0.0.1", 4242);

    inputThread.join();
    return 0;
}

void userInput(Client &client, const std::string &username) {
    while (true) {
        std::string in;
        std::getline(std::cin, in);

        if (in == "exit") {
            client.sendMessage<DisconnectionRequest>();
            client.stop();
            break;
        }

        client.sendMessage<ChatMessage>(username, in);

        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
}

void printMessage(const ChatMessage &message) {
#ifdef _WIN32
    HANDLE hConsole = GetStdHandle(STD_OUTPUT_HANDLE);
    CONSOLE_SCREEN_BUFFER_INFO consoleInfo;
    WORD saved_attributes;

    /* Save current attributes */
    GetConsoleScreenBufferInfo(hConsole, &consoleInfo);
    saved_attributes = consoleInfo.wAttributes;

    if (message.getName() == "Server") {
        SetConsoleTextAttribute(hConsole, FOREGROUND_BLUE);
    }
#endif
    std::printf("<%s> %s\n", message.getName().c_str(), message.getMessage().c_str());

#ifdef _WIN32
    /* Restore original attributes */
    SetConsoleTextAttribute(hConsole, saved_attributes);
#endif
}