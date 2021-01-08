package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.OptionDescription;

public enum RequestedIpAddressOptionDescription implements OptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 50;
    }

    @Override
    public RequestedIpAddressOption produce() {
        return new RequestedIpAddressOption();
    }
}
