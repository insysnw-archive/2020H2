package com.github.lexcorp3439.net.lab2.dhcp.util;

import org.tomlj.TomlArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OptionsReader {
    private OptionsReader() {

    }

    public static String readString(Object value) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("not a string");
        }
        return (String) value;
    }

    public static int readIPAddress(Object value) {
        return IPAddress.parse(readString(value));
    }

    public static List<Integer> readIPAddressList(Object value) {
        if (value instanceof String) {
            return Collections.singletonList(IPAddress.parse((String) value));
        } else if (value instanceof TomlArray) {
            TomlArray array = (TomlArray) value;
            if (!array.containsStrings()) {
                throw new IllegalArgumentException("TOML array does not contain strings");
            }
            final int size = array.size();
            List<Integer> result = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                result.add(IPAddress.parse(array.getString(i)));
            }
            return result;
        } else {
            throw new IllegalArgumentException("wrong TOML type");
        }
    }

    public static long readLong(Object value) {
        if (!(value instanceof Long)) {
            throw new IllegalArgumentException("wrong TOML type");
        }
        return (Long) value;
    }

    public static short readUnsignedShort(Object value) {
        final long lvalue = readLong(value);
        if (lvalue < 0 || lvalue > 0xFFFF) {
            throw new IllegalArgumentException("out of 16 bit number range");
        }
        return (short) lvalue;
    }

    public static int readUnsignedInt(Object value) {
        long lvalue = readLong(value);
        if ((lvalue & 0xFFFF_FFFF_0000_0000L) != 0L) {
            throw new IllegalArgumentException("out of 32 bit number range");
        }
        return (int) lvalue;
    }

}
