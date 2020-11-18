#include <cstdlib>
#include <iostream>
#include <fstream>
#include <sstream>

#include <ekutils/log.hpp>
#include <ekutils/resolver.hpp>

#include "zones.hpp"
#include "database.hpp"
#include "namez.hpp"
#include "packet.hpp"
#include "arguments.hpp"
#include "client.hpp"
#include "config.hpp"
#include "records/NS.hpp"

std::string loadfile(const std::string & filename) {
	std::ifstream file(filename);
	if (!file) {
		std::cerr << "configuration file \"" << filename << "\" not found" << std::endl;
		exit(EXIT_FAILURE);
	}
	std::ostringstream ss;
    ss << file.rdbuf();
    return ss.str();
}

using namespace ktlo::dns;
using namespace ktlo;

void print_usage(std::ostream & output, const std::string_view & program, const arguments & args) {
	output
		<< args.build_help(program) << std::endl
		<< R"(Zones: zones configuration (default: zones.yml), peek one of
  FILENAME   name of the file
  @VARIABLE  environment variable that holds the configuration
  -          read zones configuration from stdin
)";
}

bool is_here(const zone::addresses_t & addresses, const ekutils::net::endpoint & coresponsent) {
	switch (coresponsent.family()) {
		case ekutils::net::family_t::ipv4: {
			const auto & endpoint = dynamic_cast<const ekutils::net::ipv4::endpoint &>(coresponsent);
			for (const auto & address : addresses) {
				if (std::holds_alternative<ekutils::net::ipv4::address>(address)) {
					if (std::get<ekutils::net::ipv4::address>(address) == endpoint.address())
						return true;
				}
			}
			break;
		}
		case ekutils::net::family_t::ipv6: {
			const auto & endpoint = dynamic_cast<const ekutils::net::ipv6::endpoint &>(coresponsent);
			for (const auto & address : addresses) {
				if (std::holds_alternative<ekutils::net::ipv6::address>(address)) {
					if (std::get<ekutils::net::ipv6::address>(address) == endpoint.address())
						return true;
				}
			}
			break;
		}
		default: log_fatal("unreachable");
	}
	return false;
}

int main(int argc, char ** argv) {
	using namespace std::string_literals;

	arguments args;

	std::string configuration;

	try {
		args.parse(argc, argv);
		if (args.help) {
			print_usage(std::cout, argv[0], args);
			return EXIT_SUCCESS;
		}
		if (args.version) {
			std::cout << config::version << std::endl;
			return EXIT_SUCCESS;
		}
		if (args.single_tilda) {
			if (!args.positional.empty())
				throw ekutils::arguments_parse_error("several configurations were specified");
			std::ostringstream ss;
			ss << std::cin.rdbuf();
			configuration = ss.str();
		} else if (args.positional.empty()) {
			configuration = loadfile("zones.yml");
		} else if (args.positional.size() == 1) {
			const std::string & specification = args.positional.front();
			if (specification[0] == '@') {
				const char * env = std::getenv(specification.c_str() + 1);
				if (!env) {
					std::cerr << "environment variable \"" << (specification.c_str() + 1) << "\" not found" << std::endl;
					return EXIT_FAILURE;
				}
				configuration = env;
			} else {
				configuration = loadfile(specification);
			}
		} else {
			throw ekutils::arguments_parse_error("several configurations were specified");
		}
	} catch (const ekutils::arguments_parse_error & e) {
		std::cerr << e.what() << std::endl;
		print_usage(std::cerr, argv[0], args);
		return EXIT_FAILURE;
	} catch (const std::exception & e) {
		std::cerr << e.what() << std::endl;
		return EXIT_FAILURE;
	}

	try {
		YAML::Node node = YAML::Load(configuration);
		ekutils::log = new ekutils::stdout_log(args.log_level());
		log_info("project: " + config::project + "-dns-server, version: " + config::version);

		namez names;
		database db(names);
		read(db, node);
		log_verbose("database:\n" + db.to_string());
		auto targets = ekutils::net::resolve(ekutils::net::socket_types::datagram, args.bind);
		if (targets.empty())
			throw std::runtime_error("there is no ip address associated with " + args.bind);
		auto server = ekutils::net::bind_datagram_any(targets.begin(), targets.end());
		log_info("started server socket: " + server->local_endpoint().to_string());
		packet p(names);
		varbytes response;
		ktlo::dns::client client(names); 
		
		for (;;) {
			p.head.id = 0u;
			std::unique_ptr<ekutils::net::endpoint> coresponsent;
			std::array<byte_t, 10000> data;
			std::size_t size = server->read(data.data(), data.size(), coresponsent);
			log_info("request from " + coresponsent->to_string());
			varbytes_view actual(data.data(), size);
			try {
				p.read(actual);
				std::string dump_str = coresponsent->to_string() + "-" + std::to_string(p.head.id);
				if (args.dump)
					std::basic_ofstream<byte_t>("req-" + dump_str) << actual;
				log_debug("incoming:\n" + p.to_string());
				if (p.head.is_response) {
					throw dns_error(rcodes::format_error, "query packet expected");
				}
				p.head.is_response = true;
				p.answers.clear();
				p.head.rcode = rcodes::no_error;
				for (question & q : p.questions) {
					zone & z = db.zoneof(q.qname);
					if (!z.allowed.empty()) {
						if (!is_here(z.allowed, *coresponsent))
							throw dns_error(rcodes::refused, "corespondent not in the allow list");
					}
					if (!z.denied.empty()) {
						if (is_here(z.denied, *coresponsent))
							throw dns_error(rcodes::refused, "corespondent is in the deny list");
					}
					const auto & forward = z.forward;
					if (!forward.empty()) {
						// do recursive request
						const ekutils::uri & uri = forward[rand() % forward.size()];
						auto port = uri.get_port();
						if (port == -1)
							port = 53;
						p.head.recursion_available = true;
						client.recursive_request(p.answers, uri.get_host(), std::to_string(port), q);
					} else {
						// do database request
						question_info qinfo { q, answer_categories::regular };
						z.ask(p.answers, qinfo);
						// check authority
						if (z.is_authoritative())
							p.head.authoritative = true;
						// add name authority servers
						z.ask(p.answers, question_info { question(z.domain, records::NS::tid, record_classes::IN), answer_categories::authority });
					}
				}
				log_debug("outcoming:\n" + p.to_string());
				p.write(response);
				if (args.dump)
					std::basic_ofstream<byte_t>("res-" + dump_str) << response;
				server->write(reinterpret_cast<byte_t *>(response.data()), response.size(), *coresponsent);
			} catch (const dns_error & e) {
				log_error(e);
				packet rep(names);
				rep.head.is_response = true;
				rep.head.rcode = e.rcode;
				rep.head.id = p.head.id;
				rep.answers.clear();
				log_debug("outcoming:\n" + rep.to_string());
				rep.write(response);
				server->write(reinterpret_cast<byte_t *>(response.data()), response.size(), *coresponsent);
			}
			log_info("waiting next coresponsent...");
			names.cleanup();
		}
	} catch (const std::exception & e) {
		log_error(e);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}
