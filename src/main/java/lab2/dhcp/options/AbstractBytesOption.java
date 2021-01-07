package lab2.dhcp.options;

import lab2.dhcp.Option;
import lab2.dhcp.util.Decoder;
import lab2.dhcp.util.Encoder;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

public abstract class AbstractBytesOption implements Option {
    private static final byte[] empty = new byte[0];
    private static final Base64.Encoder b64encoder = Base64.getEncoder();

    private byte[] bytes = empty;

    public final byte[] getBytes() {
        return bytes;
    }

    public final void setBytes(byte[] value) {
        bytes = value;
    }

    @Override
    public final void encode(Encoder encoder) {
        encoder.putBytes(bytes);
    }

    @Override
    public final void decode(Decoder decoder) {
        bytes = decoder.getBytes();
    }

    @Override
    public String toString() {
        return b64encoder.encodeToString(bytes);
    }
}
