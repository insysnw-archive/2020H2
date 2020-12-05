#include "server.h"

#include "setup.h"
#include "utils.h"

#include <signal.h>

#include <memory>

std::unique_ptr<Server> server;

void signalHandler(int signal) {
    if (signal == SIGINT)
        server->stop();
}

int main(int argc, char * argv[]) {
    signal(SIGPIPE, SIG_IGN);

    auto setup = getSetup(argc, argv, "127.0.0.1", 50000);
    if (!setup.has_value())
        return EXIT_FAILURE;

    setup->eventBufferSize = 20;
    setup->timeout = 1000;

    server = std::make_unique<Server>(*setup);
    signal(SIGINT, signalHandler);
    server->start();
    server->join();

    return EXIT_SUCCESS;
}
