#include "arguments.hpp"

namespace ktlo::dns {

arguments::arguments() :
	help(add<flag>("help", [](flag & opt) {
		opt.hint = "print this help message and exit";
		opt.c = 'h';
	})),
	version(add<flag>("version", [](flag & opt) {
		opt.hint = "show version number and exit";
		opt.c = 'v';
	})),
	bind(add<string>("bind", [](string & opt) {
		opt.value = "udp://localhost";
		opt.hint = "network address to bind to";
	})),
	verb(add<variant<ekutils::log_level>>("verb", [](variant<ekutils::log_level> & opt) {
		opt.value = ekutils::log_level::info;
		opt.hint = "logger verbosity level";
		opt.variants = {
			{ "none", ekutils::log_level::none },
			{ "fatal", ekutils::log_level::fatal },
			{ "error", ekutils::log_level::error },
			{ "warning", ekutils::log_level::warning },
			{ "info", ekutils::log_level::info },
			{ "verbose", ekutils::log_level::verbose },
			{ "debug", ekutils::log_level::debug }
		};
	})),
	dump(add<flag>("dump", [](flag & opt) {
		opt.hint = "dump udp packet into files in the current directory";
		opt.c = 'D';
	})),
	debug(add<flag>("debug", [](flag & opt) {
		opt.hint = "set debug logging level";
		opt.c = 'd';
	}))
{
	positional_hint = "[zones]";
}

} // namespace ktlo::dns
