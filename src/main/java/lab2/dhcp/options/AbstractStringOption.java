package lab2.dhcp.options;

import lab2.dhcp.Option;
import lab2.dhcp.util.Decoder;
import lab2.dhcp.util.Encoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class AbstractStringOption implements Option {
    private String name;

    public final String getName() {
        return name;
    }

    public final void setName(String value) {
        name = value;
    }

    @Override
    public final void encode(Encoder encoder) {
        byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
        assert bytes.length <= Option.MAX_SIZE;
        encoder.putBytes(bytes);
    }

    @Override
    public final void decode(Decoder decoder) {
        byte[] bytes = decoder.getBytes();
        name = new String(bytes, StandardCharsets.US_ASCII);
    }

    @Override
    public String toString() {
        return '"' + name + '"';
    }
}
