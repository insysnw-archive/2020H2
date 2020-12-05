#pragma once

#include <netinet/in.h>
#include <string>

struct ConnectionSetup {
    in_port_t port;
    std::string address;
    int type;
};

struct EndpointSetup {
    ConnectionSetup connection;
    int parallelWorkers;
    int eventBufferSize;
    int timeout;
};
