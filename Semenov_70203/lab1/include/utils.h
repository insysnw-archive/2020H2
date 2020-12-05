#pragma once

#include "setup.h"

#include <optional>
#include <string>

struct ConnectionSetup;

enum class NET_ERROR : int { TEMPORARY, CRITICAL, NONE };

struct ConnectionResponse {
    explicit ConnectionResponse(int s) noexcept
        : socket{s}, error{NET_ERROR::NONE} {}

    explicit ConnectionResponse(NET_ERROR e) noexcept : socket{-1}, error{e} {}

    int socket;
    NET_ERROR error;
};

void logError(std::string_view source) noexcept;

void logInfo(std::string_view info) noexcept;

void makeNonBlocking(int fd) noexcept;

ConnectionResponse bindedSocket(const ConnectionSetup & setup) noexcept;

ConnectionResponse listeningSocket(const ConnectionSetup & setup) noexcept;

ConnectionResponse connectedSocket(const ConnectionSetup & setup) noexcept;

std::optional<EndpointSetup>
getSetup(int argc, char * argv[], std::string_view ip, int port) noexcept;
