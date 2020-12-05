#include "message.h"

#include <algorithm>
#include <cstring>

std::string Message::serialize() const noexcept {
    constexpr auto timeSize = sizeof(TimeType);
    char portableTime[timeSize];

    for (size_t i = 0; i < timeSize; ++i)
        portableTime[i] = (datetime >> (8 * i)) & 0xFF;

    std::string serialized;
    serialized.reserve(timeSize + author.size() + text.size() + 1);
    serialized.append(portableTime, timeSize);
    serialized.append(author);
    serialized.append("\n");
    serialized.append(text);
    serialized.append("\0", 1);

    return serialized;
}

Message Message::deserialize(const char * data, size_t size) noexcept {
    constexpr auto timeSize = sizeof(TimeType);

    Message deserialized;

    auto dataEnd = data + size - 1;
    auto authorStart = data + timeSize;
    auto authorEnd = std::find(authorStart, dataEnd, '\n');

    memcpy(&deserialized.datetime, data, timeSize);
    std::copy(authorStart, authorEnd, std::back_inserter(deserialized.author));
    std::copy(authorEnd + 1, dataEnd, std::back_inserter(deserialized.text));

    return deserialized;
}
