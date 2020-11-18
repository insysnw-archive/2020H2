#include <cstdlib>
#include <ctime>
#include <iostream>
#include <fstream>
#include <chrono>

#include <ekutils/udp_d.hpp>
#include <ekutils/resolver.hpp>

#include "packet.hpp"

using namespace std::chrono;
using namespace ktlo;
using namespace ktlo::ntp;

int main(int argc, char * argv[]) {
	ntp_timestamp t0 { 895658865ull, 0ull };
	std::string port = "ntp";
	std::string host_name = "ru.pool.ntp.org";

	switch (argc) {
		default:
			std::cerr << "too many arguments" << std::endl << argv[0] << " [host] [port] [t0]" << std::endl;
			exit(EXIT_FAILURE);
		case 4: t0.seconds = std::atoll(argv[3]); [[fallthrough]];
		case 3: port = argv[2]; [[fallthrough]];
		case 2: host_name = argv[1]; [[fallthrough]];
		case 1: break;
		case 0: abort();
	}

	try {
		auto infos = ekutils::net::resolve(host_name, "ntp", ekutils::net::protocols::udp);

		if (infos.empty())
			throw std::runtime_error("hostname \"" + host_name + "\" not resolved");

		auto client = ekutils::net::prepare(infos.front());

		auto server = std::move(infos.front().address);

		varbytes outcoming;

		packet p;
		p.mode = modes::client;
		p.transmit = t0;
		time_point start = std::chrono::high_resolution_clock::now();
		p.write(outcoming);
		client->write(outcoming.data(), outcoming.size(), *server);

		std::array<byte_t, 5000> incoming;
		std::size_t size = client->read(incoming.data(), incoming.size(), server);
		varbytes_view view(incoming.data(), size);
		time_point stop = std::chrono::high_resolution_clock::now();
		p.read(view);
		if (p.origin != t0)
			throw std::runtime_error("wrong timestamp");
		ntp_timestamp t3 = t0 + (stop - start);
		ntp_timestamp & t1 = p.receive;
		ntp_timestamp & t2 = p.transmit;
		ntp_timestamp theta = ((t1 - t0) + (t2 - t3)).div2();
		ntp_timestamp delta = (t3 - t0) - (t2 - t1);

		std::cout << "server: " << server->to_string() << std::endl;
		std::cout << "t0=" << t0 << "t1=" << t1 << "t2=" << t2 << "t3=" << t3;
		std::cout << "theta=" << theta.seconds << ", delta=" << delta.seconds << std::endl;
	} catch (const std::exception & error) {
		std::cerr << "error: " << error.what() << std::endl;
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}
