#include <signal.h>
#include <memory>

#include "dhcp/config.h"
#include "dhcp/dhcp_server.h"

std::unique_ptr<dhcp::DhcpServer> server;

void handler(int signal) {
    if (signal == SIGINT && server != nullptr)
        server->stop();
}

int main(int argc, char * argv[]) {
    dhcp::Config config{argc, argv};
    server = std::make_unique<dhcp::DhcpServer>(config);
    signal(SIGINT, handler);

    return EXIT_SUCCESS;
}
