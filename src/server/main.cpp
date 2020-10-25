//
// Created by Roman Svechnikov on 10.09.2020.
//

#include <iostream>
#include "Server.h"

int main() {
    Server server {4242};
    std::printf("Starting TCP server on port 4242...\n");
    server.start();
    std::printf("Server is stopped.\n");
    return 0;
}