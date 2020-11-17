#include <bits/stdint-intn.h>
#include <bits/stdint-uintn.h>
#include <netinet/ip.h>

#include <string>
#include <vector>

#include "dhcp/net_int.h"

namespace dhcp {

struct Option {
    uint8_t code;
    RawType data;
};

struct DhcpPackage {
    using OptionsList = std::vector<Option>;

    NetInt<uint8_t> op;
    NetInt<uint8_t> htype;
    NetInt<uint8_t> hlen;
    NetInt<uint8_t> hops;
    NetInt<uint32_t> xid;
    NetInt<uint16_t> secs;
    NetInt<uint16_t> flags;
    NetInt<uint32_t> ciaddr;
    NetInt<uint32_t> yiaddr;
    NetInt<uint32_t> siaddr;
    NetInt<uint32_t> giaddr;
    RawType chaddr;
    RawType sname;
    RawType file;

 public:
    RawType serialize() const noexcept;

    static DhcpPackage deserialize(const RawType & bytes) noexcept;

    void print() const noexcept;

 private:
    std::vector<Option> options;
};

}  // namespace dhcp
