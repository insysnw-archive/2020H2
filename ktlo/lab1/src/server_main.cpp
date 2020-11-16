#include <iostream>

#include <signal.h>

#include "server.hpp"
#include "server_sync.hpp"
#include "server_arguments.hpp"
#include "config.hpp"

int main(int argc, char *argv[]) {
    using namespace std::string_literals;
    using namespace ktlo::chat;
    try {
        common_args = &server_args;
        ekutils::log = new ekutils::stdout_log(ekutils::log_level::debug);
        server_args.parse(argc, argv);
		if (server_args.help) {
            std::cout << server_args.build_help(argv[0]) << std::endl;
			return EXIT_SUCCESS;
		}
		if (server_args.version) {
			std::cout << config::version << std::endl;
			return EXIT_SUCCESS;
		}
        delete ekutils::log;
        ekutils::log = new ekutils::stdout_log(server_args.log_level());
        if (server_args.sync) {
            signal(SIGPIPE, SIG_IGN);
            server_sync srv;
        } else {
            ekutils::epoll_d epoll;
            server srv(epoll);
            for (;;)
                epoll.wait(-1);
        }
    } catch (const std::invalid_argument & e) {
		std::cerr << e.what() << std::endl;
		std::cerr << server_args.build_help(argv[0]) << std::endl;
		return EXIT_FAILURE;
	} catch (const std::exception & e) {
        log_fatal("error: "s + e.what());
        return EXIT_FAILURE;
    }

	return EXIT_SUCCESS;
}
