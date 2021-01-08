package com.github.lexcorp3439.net.lab2.dhcp.options.ip;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractIpAddressOption;
import com.github.lexcorp3439.net.lab2.dhcp.util.OptionsReader;
import com.github.lexcorp3439.net.lab2.dhcp.ConfigurableOption;

public final class BroadcastAddressOption extends AbstractIpAddressOption implements ConfigurableOption {
    @Override
    public BroadcastAddressOptionDescription getDescription() {
        return BroadcastAddressOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        setAddress(OptionsReader.readIPAddress(configNode));
    }
}
