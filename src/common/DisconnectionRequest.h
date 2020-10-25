//
// Created by Roman Svechnikov on 12.09.2020.
//

#ifndef NETLABS_DISCONNECTIONREQUEST_H
#define NETLABS_DISCONNECTIONREQUEST_H


#include <utility>
#include <cstdlib>
#include <cstring>
#include "PacketHeaders.h"

class DisconnectionRequest {
public:
    [[nodiscard]] std::pair<char *, size_t> serialize() const;
};


#endif //NETLABS_DISCONNECTIONREQUEST_H
