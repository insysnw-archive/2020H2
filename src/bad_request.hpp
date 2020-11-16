#ifndef CHAT_BAD_REQUEST_HEAD_FRGHGIUYJHTGR
#define CHAT_BAD_REQUEST_HEAD_FRGHGIUYJHTGR

#include <string>
#include <stdexcept>

namespace ktlo::chat {

class bad_request : public std::runtime_error {
public:
	explicit bad_request(const std::string & message) : std::runtime_error(message) {}
	explicit bad_request(const char * message) : std::runtime_error(message) {}
};

} // namespace ktlo::chat

#endif // CHAT_BAD_REQUEST_HEAD_FRGHGIUYJHTGR
