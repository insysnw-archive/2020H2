#ifndef CLIENT_ARGUMENTS_HEAD_OOOREGVEWDFC
#define CLIENT_ARGUMENTS_HEAD_OOOREGVEWDFC

#include "arguments.hpp"

namespace ktlo::chat {

struct client_arguments : public arguments {
	std::string & username;
	std::intmax_t & pulse;

	const std::string & resolve_username() const;

	client_arguments();
};

extern client_arguments client_args;

} // namespace ktlo::chat

#endif // CLIENT_ARGUMENTS_HEAD_OOOREGVEWDFC
