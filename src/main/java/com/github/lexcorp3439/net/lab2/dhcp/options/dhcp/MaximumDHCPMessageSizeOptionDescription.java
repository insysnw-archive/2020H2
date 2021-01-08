package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.Option;
import com.github.lexcorp3439.net.lab2.dhcp.OptionDescription;

public enum MaximumDHCPMessageSizeOptionDescription implements OptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 57;
    }

    @Override
    public Option produce() {
        return new MaximumDHCPMessageSizeOption();
    }
}
