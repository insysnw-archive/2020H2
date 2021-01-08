package com.github.lexcorp3439.net.lab2.dhcp.options.vendor;

import com.github.lexcorp3439.net.lab2.dhcp.ConfigurableOptionDescription;

public enum SubnetMaskOptionDescription implements ConfigurableOptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 1;
    }

    @Override
    public String getName() {
        return "mask";
    }

    @Override
    public SubnetMaskOption produce() {
        return new SubnetMaskOption();
    }
}
