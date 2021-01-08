package com.github.lexcorp3439.net.lab2.dhcp.options.vendor;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractStringOption;
import com.github.lexcorp3439.net.lab2.dhcp.util.OptionsReader;
import com.github.lexcorp3439.net.lab2.dhcp.ConfigurableOption;

public final class HostNameOption extends AbstractStringOption implements ConfigurableOption {
    @Override
    public HostNameOptionDescription getDescription() {
        return HostNameOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        setName(OptionsReader.readString(configNode));
    }
}
