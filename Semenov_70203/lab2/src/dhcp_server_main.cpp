#include <signal.h>
#include <unistd.h>
#include <iostream>
#include <memory>

#include "dhcp/client.h"
#include "dhcp/common.h"
#include "dhcp/config.h"
#include "dhcp/dhcp_server.h"

std::unique_ptr<dhcp::DhcpServer> server;

void handler(int signal) {
    if (signal == SIGINT && server != nullptr)
        server->stop();
}

int main(int argc, char * argv[]) {
    signal(SIGINT, handler);
    dhcp::Config config{argc, argv};
    server = std::make_unique<dhcp::DhcpServer>(config);

    return EXIT_SUCCESS;
}
