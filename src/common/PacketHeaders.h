//
// Created by Roman Svechnikov on 12.09.2020.
//

#ifndef NETLABS_PACKETHEADERS_H
#define NETLABS_PACKETHEADERS_H

#include <cinttypes>

namespace headers {
    using header_t = char;
    static constexpr header_t CONNECTION_REQUEST = 0x01;
    static constexpr header_t CONNECTION_RESPONSE = 0x02;
    static constexpr header_t DISCONNECTION_REQUEST = 0x03;
    static constexpr header_t CHAT_MESSAGE = 0x10;
}

using packetlen_t = uint16_t;

#endif //NETLABS_PACKETHEADERS_H
