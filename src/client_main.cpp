#include <iostream>

#include "client.hpp"
#include "client_sync.hpp"
#include "settings.hpp"
#include "resources.hpp"
#include "config.hpp"

void print_usage(std::ostream & output, const char * program) {
	output << "Usage: " << program << res::view(res::usage::client_txt);
}

int main(int argc, char ** argv) {
	using namespace ktlo::chat;
	try {
		settings.parse_args(argc, const_cast<const char **>(argv));
		if (settings.help) {
			print_usage(std::cout, argv[0]);
			return EXIT_SUCCESS;
		}
		if (settings.version) {
			std::cout << config::version << std::endl;
			return EXIT_SUCCESS;
		}
		if (settings.sync) {
			client_sync c;
		} else {
			ekutils::epoll_d epoll;
			client c(epoll);
			for (;;)
				epoll.wait(-1);
		}
	} catch (const std::invalid_argument & e) {
		std::cerr << e.what() << std::endl;
		print_usage(std::cerr, argv[0]);
		return EXIT_FAILURE;
	} catch (const std::exception & e) {
		std::cerr << "error: " << e.what() << std::endl;
		return EXIT_FAILURE;
	}
}
