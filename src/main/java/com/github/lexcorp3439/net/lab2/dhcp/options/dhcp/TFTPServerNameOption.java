package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractStringOption;
import com.github.lexcorp3439.net.lab2.dhcp.util.OptionsReader;
import com.github.lexcorp3439.net.lab2.dhcp.ConfigurableOption;

public final class TFTPServerNameOption extends AbstractStringOption implements ConfigurableOption {
    @Override
    public TFTPServerNameOptionDescription getDescription() {
        return TFTPServerNameOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        setName(OptionsReader.readString(configNode));
    }
}
