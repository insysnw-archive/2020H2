package lab2.dhcp.util;

import java.nio.ByteBuffer;

public final class Decoder {
    private final ByteBuffer buffer;

    public Decoder(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public byte getByte() {
        return buffer.get();
    }

    public short getShort() {
        return buffer.getShort();
    }

    public int getInt() {
        return buffer.getInt();
    }

    public int getIPAddress() {
        return getInt();
    }

    public long getLong() {
        return buffer.getLong();
    }

    public void getBytes(byte[] bytes, int start, int size) {
        buffer.get(bytes, start, size);
    }

    public void getBytes(byte[] bytes) {
        buffer.get(bytes, 0, bytes.length);
    }

    public byte[] getBytes(int size) {
        byte[] result = new byte[size];
        buffer.get(result);
        return result;
    }

    public byte[] getBytes() {
        return getBytes(buffer.remaining());
    }

    public void discard(int size) {
        buffer.position(buffer.position() + size);
    }

    public int remaining() {
        return buffer.remaining();
    }
}
