#ifndef ARGUMENTS_HEAD_PPSOVKRMKDL
#define ARGUMENTS_HEAD_PPSOVKRMKDL

#include <ekutils/arguments.hpp>
#include <ekutils/log.hpp>

namespace ktlo::dns {

struct arguments : public ekutils::arguments {
	bool & help;
	bool & version;
	std::string & bind;
	ekutils::log_level & verb;
	bool & dump;
	bool & debug;

	ekutils::log_level log_level() const {
		return debug ? ekutils::log_level::debug : verb;
	}

	arguments();
};

} // namespace ktlo::dns

#endif // ARGUMENTS_HEAD_PPSOVKRMKDL
