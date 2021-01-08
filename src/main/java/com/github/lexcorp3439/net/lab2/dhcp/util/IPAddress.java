package com.github.lexcorp3439.net.lab2.dhcp.util;

public final class IPAddress {
    private IPAddress() {

    }

    private static int findOctetEnd(final String string, final int start) {
        final int end = string.length();
        for (int i = start; i < end; ++i) {
            char c = string.charAt(i);
            if (c == '.') {
                return i;
            } else if (!(c >= '0' && c <= '9')) {
                throw new IllegalArgumentException("IP address format error");
            }
        }
        return end;
    }

    public static int parse(String value) {
        int offset = 0;
        int result = 0;
        for (int i = 0; i < 4; ++i) {
            final int newOffset = findOctetEnd(value, offset);
            final String octetString = value.substring(offset, newOffset);
            final int octet = Integer.parseUnsignedInt(octetString);
            if (octet < 0 || octet > 255)
                throw new IllegalArgumentException("IP address octet value is out of bounds");
            if (value.length() != newOffset && value.charAt(newOffset) != '.') {
                throw new IllegalArgumentException("IP address has illegal character");
            }
            result = (result << 8) | (octet & 0xFF);
            offset = newOffset + 1;
        }
        return result;
    }

    public static int fromBytes(byte[] bytes) {
        return (bytes[0] << 24) | (Byte.toUnsignedInt(bytes[1]) << 16) |
                (Byte.toUnsignedInt(bytes[2]) << 8) | (Byte.toUnsignedInt(bytes[3]));
    }

    public static String ipAddressToString(int address) {
        StringBuilder result = new StringBuilder(Integer.toString(address >>> 24));
        for (int i = 16; i >= 0; i -= 8) {
            result.append('.');
            result.append((address >>> i) & 0xFF);
        }
        return result.toString();
    }


}
