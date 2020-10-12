#include <iostream>

#include <signal.h>

#include "server.hpp"
#include "server_sync.hpp"
#include "settings.hpp"
#include "resources.hpp"
#include "config.hpp"

void print_usage(std::ostream & output, const char * program) {
	output << "Usage: " << program << res::view(res::usage::server_txt);
}

int main(int argc, char *argv[]) {
    using namespace std::string_literals;
    using namespace ktlo::chat;
    try {
        ekutils::log = new ekutils::stdout_log(ekutils::log_level::debug);
        settings.parse_args(argc, const_cast<const char **>(argv));
		if (settings.help) {
			print_usage(std::cout, argv[0]);
			return EXIT_SUCCESS;
		}
		if (settings.version) {
			std::cout << config::version << std::endl;
			return EXIT_SUCCESS;
		}
        delete ekutils::log;
        ekutils::log = new ekutils::stdout_log(settings.verb);
        if (settings.sync) {
            signal(SIGPIPE, SIG_IGN);
            server_sync srv;
        } else {
            ekutils::epoll_d epoll;
            server srv(epoll);
            for (;;)
                epoll.wait(-1);
        }
    } catch (const std::exception & e) {
        log_fatal("error: "s + e.what());
        return EXIT_FAILURE;
    }

	return EXIT_SUCCESS;
}
