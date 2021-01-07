package lab2.dhcp;

import lab2.dhcp.util.Encoder;

import java.util.Objects;

public final class MACAddressClientIdentifier extends ClientIdentifier {
    private long address;

    public MACAddressClientIdentifier(long address) {
        this.address = address & 0xFFFFFF_FFFFFFL;
    }

    public MACAddressClientIdentifier(byte[] data, int start) {
        address = 0;
        for (int i = 0; i < 6; ++i) {
            address = (address << 8) | Byte.toUnsignedLong(data[start + i]);
        }
    }

    public long getAddress() {
        return address;
    }

    @Override
    public byte getType() {
        return 1;
    }

    @Override
    public int getSize() {
        return 6;
    }

    @Override
    public void put(byte[] destination, int start) {
        for (int i = 5; i >= 0; --i) {
            destination[start++] = (byte) (address >>> (i*8));
        }
    }

    @Override
    public void put(Encoder encoder) {
        for (int i = 0; i < 6; ++i) {
            encoder.putByte(octetAt(i));
        }
    }

    private static char code2hex(byte code) {
        assert (code & 0xF0) != 0;
        if (code <= 9) {
            return (char) ('0' + code);
        } else {
            return (char) ('A' + code - 10);
        }
    }

    private byte octetAt(int i) {
        return (byte) (address >>> ((5 - i)*8));
    }

    @Override
    public String toString() {
        final char[] chars = new char[17];
        final byte first = octetAt(0);
        chars[0] = code2hex((byte) ((first >>> 4) & 0xF));
        chars[1] = code2hex((byte) (first & 0xF));
        for (int i = 1; i < 6; ++i) {
            final int offset = (i - 1)*3 + 2;
            final byte octet = octetAt(i);
            chars[offset] = ':';
            chars[offset + 1] = code2hex((byte) ((octet >>> 4) & 0xF));
            chars[offset + 2] = code2hex((byte) (octet & 0xF));
        }
        return new String(chars);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof MACAddressClientIdentifier) {
            return ((MACAddressClientIdentifier) other).address == address;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    private static byte hex2code(final char c) {
        if (c >= '0' && c <= '9') {
            return (byte) (c - '0');
        } else if (c >= 'a' && c <= 'f') {
            return (byte) (c - 'a' + 10);
        } else if (c >= 'A' && c <= 'F') {
            return (byte) (c - 'A' + 10);
        } else {
            throw new IllegalArgumentException("not a hex code");
        }
    }

    private static byte parseOctet(final String string, final int start) {
        return (byte) ((hex2code(string.charAt(start)) << 4) | hex2code(string.charAt(start + 1)));
    }

    public static MACAddressClientIdentifier parse(final String string) {
        if (string.length() != 17) {
            throw new IllegalArgumentException("wrong mac address length");
        }
        long result = Byte.toUnsignedLong(parseOctet(string, 0));
        for (int i = 0; i < 5; ++i) {
            final int offset = 2 + i*3;
            final char delimiter = string.charAt(offset);
            if (delimiter != '-' && delimiter != ':') {
                throw new IllegalArgumentException("wrong character in MAC address");
            }
            byte octet = parseOctet(string, offset + 1);
            result = (result << 8) | Byte.toUnsignedLong(octet);
        }
        return new MACAddressClientIdentifier(result);
    }
}
