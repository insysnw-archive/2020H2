#include "dns_error.hpp"

namespace ktlo::dns {

const char * class_to_string(record_classes rclass) {
	switch (rclass) {
		case record_classes::IN: return "IN";
		case record_classes::CS: return "CS";
		case record_classes::CH: return "CH";
		case record_classes::HS: return "HS";
		default: return "unknown";
	}
}

const char * opcode_to_string(opcodes opcode) {
	switch (opcode) {
		case opcodes::QUERY: return "QUERY";
		case opcodes::IQUERY: return "IQUERY";
		case opcodes::STATUS: return "STATUS";
		default: return "unknown";
	}
}

const char * rcode_to_string(rcodes rcode) {
	switch (rcode) {
		case rcodes::no_error: return "no error";
		case rcodes::format_error: return "format error";
		case rcodes::server_failure: return "server failure";
		case rcodes::name_error: return "name error";
		case rcodes::not_implemented: return "not implemented";
		case rcodes::refused: return "refused";
		default: return "unknown error";
	}
}

} // ktlo::dns
