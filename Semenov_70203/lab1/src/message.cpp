#include "message.h"

#include <algorithm>
#include <cstring>
#include <string>

#include "utils.h"

template <class Type>
std::string serializeInt(Type value) {
    constexpr auto size = sizeof(Type);
    char portableValue[size];
    for (size_t i = 0; i < size; ++i)
        portableValue[i] = (value >> (8 * i)) & 0xFF;
    return std::string{portableValue, size};
}

template <class Type>
void deserializeInt(Type * value, const char * raw) {
    constexpr auto size = sizeof(Type);
    Type tmp = 0;
    for (int i = size - 1; i >= 0; --i) {
        tmp = tmp << 8;
        tmp += static_cast<unsigned char>(*(raw + i));
    }
    *value = tmp;
}

std::string Message::serialize() const noexcept {
    auto authorSize = static_cast<AuthorSizeType>(author.size());
    auto textSize = static_cast<TextSizeType>(text.size());

    auto portableTime = serializeInt(datetime);
    auto portableAuthor = serializeInt(authorSize);
    auto portableText = serializeInt(textSize);

    std::string serialized;
    serialized.append(portableTime);
    serialized.append(portableAuthor);
    serialized.append(portableText);
    serialized.append(author);
    serialized.append(text);

    return serialized;
}

std::optional<Message> Message::deserialize(
    const char * data,
    size_t size) noexcept {
    uint64_t sizeCheck = TIME_FIELD_SIZE + AUTHOR_FIELD_SIZE + TEXT_FIELD_SIZE;
    if (size < sizeCheck)
        return std::nullopt;

    auto cursor = data;
    AuthorSizeType authorSize;
    TextSizeType textSize;

    Message deserialized;
    deserializeInt(&deserialized.datetime, cursor);
    cursor += TIME_FIELD_SIZE;
    deserializeInt(&authorSize, cursor);
    cursor += AUTHOR_FIELD_SIZE;
    deserializeInt(&textSize, cursor);
    cursor += TEXT_FIELD_SIZE;

    sizeCheck += authorSize;
    sizeCheck += textSize;

    if (size < sizeCheck)
        return std::nullopt;

    std::copy(
        cursor, cursor + authorSize, std::back_inserter(deserialized.author));
    cursor += authorSize;
    std::copy(cursor, cursor + textSize, std::back_inserter(deserialized.text));

    return deserialized;
}

size_t Message::size() const noexcept {
    size_t fullSize = TIME_FIELD_SIZE + AUTHOR_FIELD_SIZE + TEXT_FIELD_SIZE;
    fullSize += text.size();
    fullSize += author.size();
    return fullSize;
}
