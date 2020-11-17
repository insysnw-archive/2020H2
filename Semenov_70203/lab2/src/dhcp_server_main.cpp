#include "dhcp/dhcp_server.h"
#include "utils.h"

#include <signal.h>

#include <memory>

std::unique_ptr<dhcp::DhcpServer> server;

void handler(int signal) {
    if (signal == SIGINT && server != nullptr)
        server->stop();
}

int main(int argc, char * argv[]) {
    auto setup = getSetup(argc, argv, "0.0.0.0", 67);
    if (!setup.has_value())
        return EXIT_FAILURE;

    setup->connection.type = SOCK_DGRAM;

    server = std::make_unique<dhcp::DhcpServer>(*setup);
    signal(SIGINT, handler);

    server->start();
    server->join();

    return EXIT_SUCCESS;
}
