#include "client.hpp"

#include <iostream>

#include <ekutils/stdin_d.hpp>

#include "protocol.hpp"
#include "settings.hpp"

namespace ktlo::chat {

client::client(ekutils::epoll_d & poll, sock_ptr && sock) : epoll(poll), input(ekutils::input), tube(std::move(sock)) {
	protocol::handshake hs;
	hs.address() = settings.resolve_address();
	hs.username() = settings.resolve_username();
	std::cerr << "login as \"" << hs.username() << "\"" << std::endl;
	tube.paket_write(hs);
	tube.socket().set_non_block();
	ekutils::input.set_non_block();
	using namespace ekutils::actions;
	epoll.add(tube.socket(), in | out | et | err | rdhup | hup, [this](ekutils::descriptor &, std::uint32_t events) {
		on_event(events);
	});
	epoll.add(ekutils::input, in | et | err | rdhup | hup, [this](ekutils::descriptor &, std::uint32_t events) {
		on_input(events);
	});
	using namespace std::chrono;
	timer.set_non_block();
	timer.period(milliseconds(settings.pulse));
	epoll.add(timer, [this](const auto &, std::uint32_t) {
		timer.read();
		tube.paket_write(protocol::noop);
	});
}

void client::on_input(std::uint32_t events) {
	namespace actions = ekutils::actions;
	try {
		if (events & (actions::rdhup | actions::hup)) {
			disconnect_self();
		}
		if (events & actions::err) {
			// disconnect with async error
			int err = errno;
			throw std::system_error(std::make_error_code(std::errc(err)), "async error");
		}
		if (events & actions::in) {
			while (process_input());
		}
	} catch (const std::exception & e) {
		std::cerr << "error: " << e.what() << std::endl;
		disconnect_self();
	}
}

bool client::process_input() {
	protocol::tell tell;
	if (!input.readln(tell.message()))
		return false;
	tube.paket_write(tell);
	return true;
}

void client::on_event(std::uint32_t events) {
	namespace actions = ekutils::actions;
	try {
		if (events & (actions::rdhup | actions::hup)) {
			disconnect_remote();
		}
		if (events & actions::err) {
			// disconnect with async error
			int err = errno;
			throw std::system_error(std::make_error_code(std::errc(err)), "async error");
		}
		if (events & actions::in) {
			tube.receive();
			while (process_response());
		}
		if (events & actions::out) {
			// out information is ready to be send
			tube.send();
		}
	} catch (const std::exception & e) {
		std::cerr << "error: " << e.what() << std::endl;
		disconnect_self();
	}
}

bool client::process_response() {
	protocol::chat chat;
	if (!tube.paket_read(chat))
		return false;
	time_t time = chat.time();
	auto tm = gmtime(&time);
	std::cout
		<< "<" << tm->tm_hour << ':' << tm->tm_min << "> ["
		<< chat.username() << "] " << chat.message() << std::endl;
	return true;
}

void client::disconnect_remote() {
	std::cerr << "remote server broke connection" << std::endl;
	std::exit(EXIT_FAILURE);
}

void client::disconnect_self() {
	std::cerr << "disconnected" << std::endl;
	std::exit(EXIT_SUCCESS);
}

client::~client() {
	epoll.remove(ekutils::input);
	epoll.remove(tube.socket());
	epoll.remove(timer);
}

} // namespace ktlo::chat
