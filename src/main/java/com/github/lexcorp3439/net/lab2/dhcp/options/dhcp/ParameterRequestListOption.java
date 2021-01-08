package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractBytesOption;

public final class ParameterRequestListOption extends AbstractBytesOption {
    @Override
    public ParameterRequestListOptionDescription getDescription() {
        return ParameterRequestListOptionDescription.INSTANCE;
    }
}
