//
// Created by Roman Svechnikov on 12.09.2020.
//

#ifndef NETLABS_CONNECTIONRESPONSE_H
#define NETLABS_CONNECTIONRESPONSE_H


#include <cstddef>
#include <utility>
#include "../common/PacketHeaders.h"

enum class ConnectionStatus {
    SUCCESS = 0,
    BAD_USERNAME = 1,
    SERVER_IS_FULL = 2
};

class ConnectionResponse {

public:
    explicit ConnectionResponse(char *data);

    explicit ConnectionResponse(ConnectionStatus status);

    [[nodiscard]] const ConnectionStatus &getStatus() const;

    [[nodiscard]] std::pair<char *, size_t> serialize() const;

private:
    ConnectionStatus status;
};


#endif //NETLABS_CONNECTIONRESPONSE_H
