#include "md5.hpp"
#include "md5_block.hpp"

#include "test.hpp"

using namespace ktlo;
using namespace ktlo::ntp;

const std::string_view test_data =
	"Man is distinguished, not only by his reason, but by this singular passion from other animals, "
	"which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable "
	"generation of knowledge, exceeds the short vehemence of any carnal pleasure.";

test {
	varbytes_view td1(reinterpret_cast<const byte_t *>(test_data.data()), test_data.size());

	block_t block { td1.substr(0, 6), 6 };
	assert_equals(0x206E614Du, block[0]);
	assert_equals(0x00807369u, block[1]);
	assert_equals(0x00000000u, block[2]);
	assert_equals(0x00000006u, block[14]);
	assert_equals(0x00000000u, block[15]);

	assert_equals("d41d8cd98f00b204e9800998ecf8427e", md5(td1.substr(0, 0)).to_string());
	assert_equals("40ae5bc1e976fdeb6f54b47ee7379361", md5(td1.substr(0, 6)).to_string());
	assert_equals("cc5bf212320bfb562bf7111ab0ab4bf9", md5(td1).to_string());
}
