#include <cstdlib>
#include <iostream>
#include <fstream>
#include <sstream>

#include <ekutils/log.hpp>
#include <ekutils/udp_d.hpp>

#include "zones.hpp"
#include "database.hpp"
#include "namez.hpp"
#include "packet.hpp"
#include "settings.hpp"
#include "config.hpp"

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

int main(int argc, char ** argv) {
	using namespace ktlo::dns;
	using namespace ktlo;
	using namespace std::string_literals;

	std::string configuration;
	switch (argc) {
		case 0: abort();
		case 1:
			configuration = loadfile("dns.yml");
			break;
		case 2: {
			const char * arg = argv[1];
			if (arg == "-"s) {
				std::ostringstream ss;
				ss << std::cin.rdbuf();
				configuration = ss.str();
			} else if (arg[0] == '@') {
				const char * env = std::getenv(arg + 1);
				if (!env) {
					std::cerr << "environment variable \"" << (arg + 1) << "\" not found" << std::endl;
					return EXIT_FAILURE;
				}
				configuration = env;
			} else {
				configuration = loadfile(arg);
			}
			break;
		}
		default:
			std::cerr << "too many arguments" << std::endl;
			return EXIT_FAILURE;
	}
	try {
		YAML::Node node = YAML::Load(configuration);
		settings conf;
		conf.apply(node["$dns"]);
		ekutils::log = new ekutils::stdout_log(conf.verb);
		log_info("project: " + config::project + "-dns-server, version: " + config::version);
		database db = read(gloabl_names, node);
		log_verbose("database:\n" + db.to_string());
		ekutils::udp_socket_d server(conf.address, conf.port);
		log_info("started server socket: [" + conf.address + "]:" + conf.port);
		packet p;
		varbytes response;
		for (;;) {
			p.head.id = 0u;
			ekutils::endpoint_info client;
			std::array<byte_t, 10000> data;
			std::size_t size = server.read(data.data(), data.size(), &client);
			log_info("request from "s + std::string(client));
			varbytes_view actual(data.data(), size);
			try {
				p.read(gloabl_names, actual);
				std::string dump_str = client.address() + "." + std::to_string(client.port()) + "-" + std::to_string(p.head.id);
				if (conf.dump)
					std::basic_ofstream<byte_t>("req-" + dump_str) << actual;
				log_verbose("incoming:\n" + p.to_string());
				if (p.head.is_response) {
					throw dns_error(rcodes::format_error, "query packet expected");
				}
				p.head.is_response = true;
				p.answers = db.ask(p.questions);
				p.authority.clear();
				p.head.rcode = rcodes::no_error;

				log_verbose("outcoming:\n" + p.to_string());
				p.write(gloabl_names, response);
				if (conf.dump)
					std::basic_ofstream<byte_t>("res-" + dump_str) << response;
				server.write(reinterpret_cast<byte_t *>(response.data()), response.size(), client);
			} catch (const dns_error & e) {
				log_error(e);
				packet rep;
				rep.head.is_response = true;
				rep.head.rcode = e.rcode;
				rep.head.id = p.head.id;
				rep.answers.clear();
				rep.authority.clear();
				rep.additional.clear();
				log_verbose("outcoming:\n" + rep.to_string());
				rep.write(gloabl_names, response);
				server.write(reinterpret_cast<byte_t *>(response.data()), response.size(), client);
			}
			log_info("waiting next client...");
			gloabl_names.cleanup();
		}
	} catch (const std::exception & e) {
		log_error(e);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}
