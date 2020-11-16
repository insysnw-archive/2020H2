#ifndef SERVER_ARGUMENTS_HEAD_WDSDRPMLVXC
#define SERVER_ARGUMENTS_HEAD_WDSDRPMLVXC

#include <ekutils/log.hpp>

#include "arguments.hpp"

namespace ktlo::chat {

struct server_arguments : public arguments {
	std::intmax_t & max_client_count;
	std::intmax_t & timeout;
	bool & single_login;

	ekutils::log_level & verb;
	bool & debug;

	ekutils::log_level log_level() const {
		return debug ? ekutils::log_level::debug : verb;
	}
	
	server_arguments();
};

extern server_arguments server_args;

} // namespace ktlo::chat

#endif // SERVER_ARGUMENTS_HEAD_WDSDRPMLVXC
