#pragma once

#include <string>

struct Message {
    using TimeType = int64_t;

    TimeType datetime;
    std::string text;
    std::string author;

    std::string serialize() const noexcept;
    static Message deserialize(const char * data, size_t size) noexcept;
};
