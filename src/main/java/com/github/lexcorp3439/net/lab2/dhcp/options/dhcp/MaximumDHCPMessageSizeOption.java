package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.util.Decoder;
import com.github.lexcorp3439.net.lab2.dhcp.util.Encoder;
import com.github.lexcorp3439.net.lab2.dhcp.Option;

public final class MaximumDHCPMessageSizeOption implements Option {
    private short size;

    public int getSize() {
        return size & 0xFFFF;
    }

    public void setSize(int value) {
        if (value < 0 || value >= 0xFFFF)
            throw new IllegalArgumentException("size should be in range [0:65535]");
        size = (short) value;
    }

    @Override
    public MaximumDHCPMessageSizeOptionDescription getDescription() {
        return MaximumDHCPMessageSizeOptionDescription.INSTANCE;
    }

    @Override
    public void encode(Encoder encoder) {
        encoder.putShort(size);
    }

    @Override
    public void decode(Decoder decoder) {
        size = decoder.getShort();
    }
}
