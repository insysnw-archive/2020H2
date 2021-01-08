package com.github.lexcorp3439.net.lab2.dhcp.options.vendor;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractIpAddressOption;
import com.github.lexcorp3439.net.lab2.dhcp.util.OptionsReader;
import com.github.lexcorp3439.net.lab2.dhcp.ConfigurableOption;

public final class SubnetMaskOption extends AbstractIpAddressOption implements ConfigurableOption {
    @Override
    public SubnetMaskOptionDescription getDescription() {
        return SubnetMaskOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        setAddress(OptionsReader.readIPAddress(configNode));
    }
}
