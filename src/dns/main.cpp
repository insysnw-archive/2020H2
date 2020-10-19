#include <iostream>

#include "config.hpp"

int main(int argc, char *argv[]) {
	std::cout
		<< "group: " << config::group << std::endl
		<< "project: " << config::project << std::endl
		<< "version: " << config::version << std::endl
		<< "maintainer: " << config::maintainer << std::endl
		<< "build: " << config::build << std::endl
		<< "-------------------------------------------" << std::endl
		<< "run cmd: ";
	for (int i = 0; i < argc; i++)
		std::cout << argv[i] << ' ';
	std::cout << std::endl;
	return 0;
}
