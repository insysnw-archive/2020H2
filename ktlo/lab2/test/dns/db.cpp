#include <iostream>

#include "database.hpp"
#include "zones.hpp"
#include "namez.hpp"

#include <ekutils/log.hpp>

#include "test.hpp"

const std::string test_data = R"!23(
$ttl: 3322

$dns:
    verb: debug

finger-food.ru.:
    $ttl: 4444
    : !MX 5 mx

handtruth.com:
    : !AAAA
    - 2002:58c9:ea93:0:0:0:0:0
    - 0:0:0:0:0:FFFF:58C9:EA93
    : !A 88.201.234.147
    mc: !CNAME
    : !MX 10 mx.yandex.net.
    : !MX
        priority: 22
        exchange: mx
    mx: !CNAME
    : !NS
    - ns1
    - ns2
    - ns3
    ns1: !CNAME
    ns2: !A
        address: 93.100.1.66
    ns3: !A 94.19.255.12
    147.234.201.88.in-addr.arpa.: !PTR
    : !SOA
        origin: ns1
        admin: admin
        serial: 20060205
        refresh: 3600
        retry: 900
        expire: 3600000
        minimum: 3600
    _mcsman._tcp: !SRV 10 30 1337 mc
    _mcsftp._udp: !SRV
        priority: 10
        weight: 30
        port: 9645
        target: ftp.mc
    text: !TXT [ Several, TXT, records ]
    texts: !TXT
        text: [ One, TXT, record, with, multiple, values ]
    dev:
        : !A 23.45.0.1
)!23";

test {
	ekutils::log = new ekutils::stdout_log(ekutils::log_level::debug);
	using namespace ktlo::dns;
	YAML::Node node = YAML::Load(test_data);
	namez ns;
	database db = read(ns, node);
	for (const auto & item : db.all()) {
		std::cout << item.first.domain() << " " << item.second->to_string() << std::endl;
	}
}
