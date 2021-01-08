package com.github.lexcorp3439.net.lab2.dhcp.options.vendor;

import com.github.lexcorp3439.net.lab2.dhcp.ConfigurableOptionDescription;

public enum RouterOptionDescription implements ConfigurableOptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 3;
    }

    @Override
    public String getName() {
        return "router";
    }

    @Override
    public RouterOption produce() {
        return new RouterOption();
    }
}
