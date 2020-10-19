#include "packet.hpp"

#include "ipv6.hpp"

#include "test.hpp"

test {
    using namespace ktlo::dns;
    ipv6 ip = ipv6::parse("2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d");
    assert_equals("2001:db8:11a3:9d7:1f34:8a2e:7a0:765d", ip.to_string());
}
