#ifndef ARGUMENTS_HEAD_PPPCESFERGF
#define ARGUMENTS_HEAD_PPPCESFERGF

#include <ekutils/arguments.hpp>

namespace ktlo::chat {

struct arguments : public ekutils::arguments {
	std::string & address;
	std::string & port;
	bool & version;
	bool & help;
	bool & sync;

	bool & is_tcp;
	bool & is_unix;

	std::intmax_t & max_paket_size;

	enum class transports {
		tcp, un
	};

	transports protocol() const;
	const std::string & resolve_address() const;

protected:
	std::string * address_hint;
	std::string * port_hint;

	arguments();
};

extern arguments * common_args;

} // namespace ktlo::chat

#endif // ARGUMENTS_HEAD_PPPCESFERGF
