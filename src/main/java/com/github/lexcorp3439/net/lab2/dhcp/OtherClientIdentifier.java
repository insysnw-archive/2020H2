package com.github.lexcorp3439.net.lab2.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.util.Encoder;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public final class OtherClientIdentifier extends ClientIdentifier {
    private static final Base64.Encoder b63enc = Base64.getEncoder();

    private final byte type;
    private final byte[] identifier;

    public OtherClientIdentifier(byte type, byte[] identifier, int start, int size) {
        this.type = type;
        this.identifier = new byte[size];
        System.arraycopy(identifier, start, this.identifier, 0, size);
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public int getSize() {
        return identifier.length;
    }

    @Override
    public void put(byte[] destination, int start) {
        System.arraycopy(identifier, 0, destination, start, identifier.length);
    }

    @Override
    public void put(Encoder encoder) {
        encoder.putBytes(identifier);
    }

    @Override
    public String toString() {
        return Byte.toUnsignedInt(type) + ':' + b63enc.encodeToString(identifier);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof OtherClientIdentifier)) return false;
        OtherClientIdentifier that = (OtherClientIdentifier) other;
        return type == that.type && Arrays.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type);
        result = 31 * result + Arrays.hashCode(identifier);
        return result;
    }
}
