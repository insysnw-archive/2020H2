package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractStringOption;

public final class BootFileNameOption extends AbstractStringOption {
    @Override
    public BootFileNameOptionDescription getDescription() {
        return BootFileNameOptionDescription.INSTANCE;
    }
}
