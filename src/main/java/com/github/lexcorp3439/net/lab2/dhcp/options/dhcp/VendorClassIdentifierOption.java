package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractStringOption;

public final class VendorClassIdentifierOption extends AbstractStringOption {
    @Override
    public VendorClassIdentifierOptionDescription getDescription() {
        return VendorClassIdentifierOptionDescription.INSTANCE;
    }
}
