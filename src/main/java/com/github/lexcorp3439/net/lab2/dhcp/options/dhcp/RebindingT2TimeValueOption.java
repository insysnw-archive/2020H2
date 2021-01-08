package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractTimeOption;

public final class RebindingT2TimeValueOption extends AbstractTimeOption {
    @Override
    public RebindingT2TimeValueOptionDescription getDescription() {
        return RebindingT2TimeValueOptionDescription.INSTANCE;
    }
}
