//
// Created by Roman Svechnikov on 10.09.2020.
//

#include "Server.h"

Server::Server(uint16_t port) : port(port) {
#ifdef _WIN32
    WSADATA wsaData;
    if (WSAStartup(0x0202, &wsaData) != 0) {
        std::fprintf(stderr, "ERROR! Where: WSAStartup\n");
        exit(1);
    }
#endif
}

Server::~Server() {
#ifdef _WIN32
    WSACleanup();
#endif
}

void Server::start() {
    std::thread inputThread([this] { userInput(); });

    sockaddr_in address{};
#ifdef _WIN32
    address.sin_addr.S_un.S_addr = INADDR_ANY;
#else
    address.sin_addr.s_addr = INADDR_ANY;
#endif
    address.sin_port = htons(port);
    address.sin_family = AF_INET;

    listenSocket = socket(AF_INET, SOCK_STREAM, 0);

    if (listenSocket < 0) {
        std::fprintf(stderr, "ERROR! Where: socket init\n");
        inputThread.join();
        stop();
        exit(1);
    }

#ifdef _WIN32
    u_long mode = 1;
    ioctlsocket(listenSocket, FIONBIO, &mode);
#else
    fcntl(listenSocket, F_SETFL, O_NONBLOCK);
#endif

    if (bind(listenSocket, (const sockaddr *) &address, sizeof(address)) < 0) {
        std::fprintf(stderr, "ERROR! Where: socket bind\n");
        inputThread.join();
        stop();
        exit(1);
    }

    if (listen(listenSocket, SOMAXCONN) < 0) {
        std::fprintf(stderr, "ERROR! Where: socket listen\n");
        inputThread.join();
        stop();
        exit(1);
    }

    loop();

    inputThread.join();
    stop();
}

void Server::loop() {
    while (true) {
        if (shouldExit) break;

        sockaddr_in client_addr;
        int addrLength = sizeof(sockaddr_in);
        socket_t clientSocket = accept(listenSocket, (sockaddr *) &client_addr, (socklen_t *) &addrLength);
        if (clientSocket != -1) {
            printf("[INFO] New connection\n");
            handlerThreads[clientSocket] = std::thread([=] {
                handleClient(clientSocket);
                clientsToFree.push(clientSocket);
            });
        }
        while (!clientsToFree.empty()) {
            auto client = clientsToFree.front();
#ifdef _WIN32
            closesocket(client);
#else
            close(client);
#endif
            handlerThreads.at(client).join();
            handlerThreads.erase(client);
            clientsToFree.pop();
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
}

void Server::stop() {
    printf("Stopping the server...\n");
#ifdef _WIN32
    closesocket(listenSocket);
#else
    close(listenSocket);
#endif
}

void Server::userInput() {
    while (true) {
        std::string in;
        std::getline(std::cin, in);

        if (in == "/stop") {
            shouldExit = true;
            break;
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
}


void Server::handleClient(socket_t clientSocket) {
    char buffer[bufferSize]; // recv buffer with a fixed length
    std::vector<char> msg; // in case the buffer is too small for a message
    std::string username;

    while (true) {
        recv(clientSocket, buffer, bufferSize, 0);

        if (buffer[0] == 0) {
            continue;
        }

        packetlen_t dataLength;
        memcpy(&dataLength, buffer + sizeof(headers::header_t), sizeof(packetlen_t));

        if (dataLength < bufferSize) {
            msg.assign(buffer, buffer + dataLength);
        } else {
            msg.assign(buffer, buffer + bufferSize);
            int bytesToRead = dataLength - bufferSize;
            while (bytesToRead > 0) {
                memset(&buffer, 0, bufferSize);
                recv(clientSocket, buffer, bufferSize, 0);
                if (bytesToRead < bufferSize) {
                    msg.insert(msg.end(), buffer, buffer + bytesToRead);
                } else {
                    msg.insert(msg.end(), buffer, buffer + bufferSize);
                }
                bytesToRead -= bufferSize;
            }
        }

        switch (msg[0]) {
            case headers::CONNECTION_REQUEST: {
                std::string name = ConnectionRequest(msg.data()).getName();
                ConnectionStatus status;
                if (users.find(name) == users.end()) {
                    users.insert(name);
                    username = name;
                    status = ConnectionStatus::SUCCESS;
                } else {
                    status = ConnectionStatus::BAD_USERNAME;
                }
                sendMessage<ConnectionResponse>(clientSocket, status);

                // A little delay to make sure that connection status will be received first
                std::this_thread::sleep_for(std::chrono::milliseconds(1));

                if (status != ConnectionStatus::SUCCESS) {
                    return;
                } else {
                    sendMessage<ChatMessage>(clientSocket, "Server",
                                             "Welcome to the chat! Praise our dear leader!");
                    for (const auto &[otherClient, thread]: handlerThreads) {
                        if (otherClient != clientSocket) {
                            sendMessage<ChatMessage>(otherClient, "Server",
                                                     username + " joined the chat. Don't forget to say hello!");
                        }
                    }
                }
                break;
            }
            case headers::DISCONNECTION_REQUEST: {
                printf("[INFO] %s disconnected.\n", username.c_str());
                for (const auto &[otherClient, thread]: handlerThreads) {
                    if (otherClient != clientSocket) {
                        sendMessage<ChatMessage>(otherClient, "Server", username + " left the chat. Bye!");
                    }
                }
                users.erase(username);
                return;
            }
            case headers::CHAT_MESSAGE: {
                auto message = ChatMessage(msg.data());
                std::printf("<%s> %s\n", message.getName().c_str(), message.getMessage().c_str());
                for (const auto &[otherClient, thread]: handlerThreads) {
                    if (otherClient != clientSocket) {
                        sendMessage<ChatMessage>(otherClient, message.getName(), message.getMessage());
                    }
                }
                break;
            }
            default:
                break;
        }
        memset(&buffer, 0, bufferSize);
        msg.clear();
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

}

