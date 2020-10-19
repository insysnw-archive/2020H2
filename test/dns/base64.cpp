#include "base64.hpp"

#include "test.hpp"

const std::string_view test_data =
	"Man is distinguished, not only by his reason, but by this singular passion from other animals, "
	"which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable "
	"generation of knowledge, exceeds the short vehemence of any carnal pleasure.";

const std::string expcetations =
	"TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0"
	"aGlzIHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1"
	"c3Qgb2YgdGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0"
	"aGUgY29udGludWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdl"
	"LCBleGNlZWRzIHRoZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=";

using namespace ktlo;
using namespace ktlo::dns;

test {
	varbytes_view td1(reinterpret_cast<const byte_t *>(test_data.data()), test_data.size());
	std::string actual = base64(td1);
	assert_equals(expcetations, actual);

	std::array<byte_t, 4u> td2 { '1', '2', '3', '4' };
	assert_equals("MTIz", base64(varbytes_view(td2.data(), 3)));
	assert_equals("MTIzNA==", base64(varbytes_view(td2.data(), 4)));
}
