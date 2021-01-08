package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractTimeOption;

public final class RenewalT1TimeValueOption extends AbstractTimeOption {
    @Override
    public RenewalT1TimeValueOptionDescription getDescription() {
        return RenewalT1TimeValueOptionDescription.INSTANCE;
    }
}
