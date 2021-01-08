package com.github.lexcorp3439.net.lab2.dhcp.options.ip;

import com.github.lexcorp3439.net.lab2.dhcp.util.Decoder;
import com.github.lexcorp3439.net.lab2.dhcp.util.Encoder;
import com.github.lexcorp3439.net.lab2.dhcp.util.OptionsReader;
import com.github.lexcorp3439.net.lab2.dhcp.ConfigurableOption;

public final class InterfaceMTUOption implements ConfigurableOption {
    private short mtu;

    public int getMTU() {
        return mtu & 0xFFFF;
    }

    public void setMTU(int value) {
        if (value < 0 || value >= 0xFFFF)
            throw new IllegalArgumentException("MTU value should be in range [0:65535]");
        mtu = (short) value;
    }

    @Override
    public InterfaceMTUOptionDescription getDescription() {
        return InterfaceMTUOptionDescription.INSTANCE;
    }

    @Override
    public void encode(Encoder encoder) {
        encoder.putShort(mtu);
    }

    @Override
    public void decode(Decoder decoder) {
        mtu = decoder.getShort();
    }

    @Override
    public void configure(Object configNode) {
        mtu = OptionsReader.readUnsignedShort(configNode);
    }
}
