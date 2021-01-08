package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.OptionDescription;

public enum ServerIdentifierOptionDescription implements OptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 54;
    }

    @Override
    public ServerIdentifierOption produce() {
        return new ServerIdentifierOption();
    }
}
