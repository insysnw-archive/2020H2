package com.github.lexcorp3439.net.lab2.dhcp.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Encoder {
    private final ByteBuffer buffer;

    public Encoder(ByteBuffer buffer) {
        buffer.order(ByteOrder.BIG_ENDIAN);
        this.buffer = buffer;
    }

    public void putByte(byte value) {
        buffer.put(value);
    }

    public void putShort(short value) {
        buffer.putShort(value);
    }

    public void putInt(int value) {
        buffer.putInt(value);
    }

    public void putIPAddress(int value) { putInt(value); }

    public void putLong(long value) {
        buffer.putLong(value);
    }

    public void putBytes(byte[] value, int start, int length) {
        buffer.put(value, start, length);
    }

    public void putBytes(byte[] value) {
        buffer.put(value, 0, value.length);
    }

    public int size() {
        return buffer.capacity();
    }
}
