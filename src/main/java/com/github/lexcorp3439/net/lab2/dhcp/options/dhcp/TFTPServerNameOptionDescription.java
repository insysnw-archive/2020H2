package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.ConfigurableOptionDescription;

public enum TFTPServerNameOptionDescription implements ConfigurableOptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 66;
    }

    @Override
    public String getName() {
        return "tftp-server-name";
    }

    @Override
    public TFTPServerNameOption produce() {
        return new TFTPServerNameOption();
    }
}
