//
// Created by Roman Svechnikov on 15.01.2021.
//

#ifndef NETLAB2_QUERYMESSAGE_H
#define NETLAB2_QUERYMESSAGE_H

#include "Message.h"

class QueryMessage : public Message {

public:
    QueryMessage(const char *buffer, long size);

    [[nodiscard]] const std::vector<Query> &getQueries() const;

private:
    std::vector<Query> queries{};

    int decodeQuery(const char *buffer);

};


#endif //NETLAB2_QUERYMESSAGE_H
