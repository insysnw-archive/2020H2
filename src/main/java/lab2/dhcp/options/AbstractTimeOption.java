package lab2.dhcp.options;

import lab2.dhcp.Option;
import lab2.dhcp.util.Decoder;
import lab2.dhcp.util.Encoder;

import java.nio.ByteBuffer;

public abstract class AbstractTimeOption implements Option {
    private int seconds;

    public final long getSeconds() {
        return seconds & 0xFF_FF_FF_FFL;
    }

    public final void setSeconds(long value) {
        seconds = (int) value;
    }

    @Override
    public final void encode(Encoder encoder) {
        encoder.putInt(seconds);
    }

    @Override
    public final void decode(Decoder decoder) {
        seconds = decoder.getInt();
    }

    @Override
    public String toString() {
        return Integer.toUnsignedString(seconds) + 's';
    }
}
