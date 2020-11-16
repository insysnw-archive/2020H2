#include <iostream>

#include "client.hpp"
#include "client_sync.hpp"
#include "client_arguments.hpp"
#include "config.hpp"

int main(int argc, char ** argv) {
	using namespace ktlo::chat;
	try {
        common_args = &client_args;
		client_args.parse(argc, argv);
		if (client_args.help) {
			std::cout << client_args.build_help(argv[0]) << std::endl;
			return EXIT_SUCCESS;
		}
		if (client_args.version) {
			std::cout << config::version << std::endl;
			return EXIT_SUCCESS;
		}
		if (client_args.sync) {
			client_sync c;
		} else {
			ekutils::epoll_d epoll;
			client c(epoll);
			for (;;)
				epoll.wait(-1);
		}
	} catch (const std::invalid_argument & e) {
		std::cerr << e.what() << std::endl;
		std::cerr << client_args.build_help(argv[0]) << std::endl;
		return EXIT_FAILURE;
	} catch (const std::exception & e) {
		std::cerr << "error: " << e.what() << std::endl;
		return EXIT_FAILURE;
	}
}
