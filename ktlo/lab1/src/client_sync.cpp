#include "client_sync.hpp"

#include <thread>
#include <iostream>

#include "protocol.hpp"
#include "settings.hpp"

namespace ktlo::chat {

client_sync::client_sync(sock_ptr && sock) : tube(std::move(sock)) {
    const std::string & username = settings.resolve_username();
	std::cerr << "login as \"" << username << "\"" << std::endl;
    protocol::handshake hs;
    hs.username() = username;
	hs.address() = settings.resolve_address();
    tube.paket_write(hs);
	std::thread read_line([this] {
        std::string line;
        while (std::cin) {
            std::getline(std::cin, line);
            protocol::tell msg;
            msg.message() = std::move(line);
            std::lock_guard lock(mutex);
            tube.paket_write(msg);
        }
        std::cerr << "disconnected" << std::endl;
	    std::exit(EXIT_SUCCESS);
    });
    std::thread pulsar;
    if (settings.pulse != 0)
        pulsar = std::thread([this] {
            for (;;) {
                std::this_thread::sleep_for(std::chrono::milliseconds(settings.pulse));
                tube.paket_write(protocol::noop);
            }
        });
    for (;;) {
        protocol::chat chat;
        tube.paket_read(chat);
        time_t time = chat.time();
        auto tm = gmtime(&time);
        std::cout
            << "<" << tm->tm_hour << ':' << tm->tm_min << "> ["
            << chat.username() << "] " << chat.message() << std::endl;
    }
}

} // namespace ktlo::chat
