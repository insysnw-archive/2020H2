//
// Created by Roman Svechnikov on 15.01.2021.
//

#ifndef NETLAB2_RESPONSEMESSAGE_H
#define NETLAB2_RESPONSEMESSAGE_H

#include "Message.h"

class ResponseMessage : public Message {

public:
    explicit ResponseMessage(uint16_t id);

    void addResourceRecord(const ResourceRecord &record);

    [[nodiscard]] std::pair<char *, size_t> serialize() const;

private:
    std::vector<ResourceRecord> records{};

    int encodeResourceRecord(const ResourceRecord &record, char *buffer) const;

};


#endif //NETLAB2_RESPONSEMESSAGE_H
