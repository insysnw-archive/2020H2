#pragma once

#include <optional>
#include <string>

struct Message {
 private:
    using DatetimeType = uint64_t;
    using AuthorSizeType = uint16_t;
    using TextSizeType = uint32_t;
    static constexpr auto TIME_FIELD_SIZE = sizeof(DatetimeType);
    static constexpr auto AUTHOR_FIELD_SIZE = sizeof(AuthorSizeType);
    static constexpr auto TEXT_FIELD_SIZE = sizeof(TextSizeType);

 public:
    DatetimeType datetime;
    std::string text;
    std::string author;

 public:
    std::string serialize() const noexcept;

    static std::optional<Message> deserialize(
        const char * data,
        size_t size) noexcept;

    size_t size() const noexcept;
};
