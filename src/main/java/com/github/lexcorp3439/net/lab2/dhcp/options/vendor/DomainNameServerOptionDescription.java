package com.github.lexcorp3439.net.lab2.dhcp.options.vendor;

import com.github.lexcorp3439.net.lab2.dhcp.ConfigurableOptionDescription;

public enum DomainNameServerOptionDescription implements ConfigurableOptionDescription {
    INSTANCE;

    @Override
    public byte getType() {
        return 6;
    }

    @Override
    public String getName() {
        return "domain-nameserver";
    }

    @Override
    public DomainNameServerOption produce() {
        return new DomainNameServerOption();
    }
}
